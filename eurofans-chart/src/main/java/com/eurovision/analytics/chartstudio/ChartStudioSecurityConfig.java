package com.eurovision.analytics.chartstudio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

/**
 * Chart-studio is the only password-gated surface this application exposes
 * (the OAuth callbacks and Telegram long-polling stay unauthenticated by
 * design, spec 4.6). Everything else keeps working exactly as before --
 * this filter chain only ever narrows {@code /chart-studio/**} and
 * {@code /api/chart-studio/**}, never anything outside those two prefixes.
 *
 * <p>Fails closed at boot (spec-consistent with {@code DeploymentBindingVerifier}):
 * an unset username/password would otherwise mean Spring Security's
 * auto-generated random password prints once to the log and nobody can log
 * in reliably, or worse, a blank password some client happens to send.
 */
@Configuration
@EnableWebFluxSecurity
public class ChartStudioSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ChartStudioSecurityConfig.class);

    @Bean
    public PasswordEncoder chartStudioPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.core.userdetails.ReactiveUserDetailsService chartStudioUserDetailsService(
            ChartStudioProperties properties, PasswordEncoder encoder) {
        if (properties.username() == null || properties.username().isBlank()
                || properties.password() == null || properties.password().isBlank()) {
            log.error("CHART_STUDIO_USERNAME / CHART_STUDIO_PASSWORD are not configured -- "
                    + "chart-studio would otherwise be reachable with no real credential. Refusing to start.");
            throw new IllegalStateException(
                    "chart-studio.username and chart-studio.password must both be set (CHART_STUDIO_USERNAME / CHART_STUDIO_PASSWORD)");
        }
        UserDetails user = User.withUsername(properties.username())
                .password(encoder.encode(properties.password()))
                .roles("CHART_STUDIO")
                .build();
        return username -> username.equals(user.getUsername()) ? Mono.just(user) : Mono.empty();
    }

    @Bean
    public SecurityWebFilterChain chartStudioFilterChain(ServerHttpSecurity http) {
        var protectedPaths = ServerWebExchangeMatchers.pathMatchers("/chart-studio/**", "/api/chart-studio/**");
        http
                .securityMatcher(protectedPaths)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpBasicServerAuthenticationEntryPoint()))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }
}
