package com.eurovision.analytics.oauth.applemusic;

import com.eurovision.analytics.config.ProviderProperties;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generates the MusicKit "developer token" (an ES256 JWT signed with the
 * Apple Developer .p8 private key). Generated ahead of time and cached, not
 * on every request (spec 4.3.1): a background check regenerates it once it is
 * within 7 days of expiry. Apple allows a maximum lifetime of ~6 months
 * (15,777,000 seconds); this service uses 175 days to stay safely under that.
 */
@Service
public class AppleMusicDeveloperTokenService {

    private static final Logger log = LoggerFactory.getLogger(AppleMusicDeveloperTokenService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofDays(175);
    private static final Duration REGENERATE_WITHIN = Duration.ofDays(7);

    private final ProviderProperties.AppleMusic config;
    private final AtomicReference<CachedToken> cached = new AtomicReference<>();

    public AppleMusicDeveloperTokenService(ProviderProperties properties) {
        this.config = properties.appleMusic();
    }

    public boolean isConfigured() {
        return config != null && config.isConfigured();
    }

    @PostConstruct
    void generateOnStartupIfConfigured() {
        if (isConfigured()) {
            regenerate();
        }
    }

    /** Daily check; only regenerates when the cached token is within 7 days of expiry. */
    @Scheduled(fixedRate = 24, timeUnit = java.util.concurrent.TimeUnit.HOURS)
    void refreshIfNeeded() {
        if (!isConfigured()) {
            return;
        }
        CachedToken current = cached.get();
        if (current == null || Instant.now().isAfter(current.expiresAt.minus(REGENERATE_WITHIN))) {
            regenerate();
        }
    }

    public String currentToken() {
        if (!isConfigured()) {
            throw new IllegalStateException("Apple Music is not configured");
        }
        CachedToken current = cached.get();
        if (current == null) {
            regenerate();
            current = cached.get();
        }
        return current.token;
    }

    private synchronized void regenerate() {
        try {
            ECPrivateKey privateKey = parsePrivateKey(config.privateKey());
            Instant now = Instant.now();
            Instant expiry = now.plus(TOKEN_LIFETIME);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .keyID(config.keyId())
                    .build();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(config.teamId())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(privateKey));

            cached.set(new CachedToken(jwt.serialize(), expiry));
            log.info("Apple Music developer token (re)generated, expires {}", expiry);
        } catch (Exception e) {
            log.error("Failed to generate Apple Music developer token: {}", e.getMessage(), e);
        }
    }

    private static ECPrivateKey parsePrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
