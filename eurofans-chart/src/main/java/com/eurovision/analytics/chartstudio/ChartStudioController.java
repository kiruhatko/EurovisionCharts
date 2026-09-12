package com.eurovision.analytics.chartstudio;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Auth for everything under {@code /api/chart-studio/**} is enforced by
 * {@link ChartStudioSecurityConfig}, not here -- this controller only ever
 * needs to worry about the data.
 */
@RestController
public class ChartStudioController {

    private final ChartStudioService chartStudioService;

    public ChartStudioController(ChartStudioService chartStudioService) {
        this.chartStudioService = chartStudioService;
    }

    // Spring's static-resource handler only auto-serves index.html for the
    // context root ("/"), not for a static subdirectory -- so a bare
    // /chart-studio or /chart-studio/ would otherwise 404.
    @GetMapping({"/chart-studio", "/chart-studio/"})
    public Mono<Void> redirectToIndex(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/chart-studio/index.html"));
        return response.setComplete();
    }

    @GetMapping("/api/chart-studio/chart")
    public ChartStudioDtos.Response chart(@RequestParam(name = "weekOffset", defaultValue = "0") int weekOffset) {
        return chartStudioService.chartForWeek(Math.max(0, weekOffset));
    }
}
