package com.eurovision.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(String tokenEncryptionKey) {

    public boolean isConfigured() {
        return tokenEncryptionKey != null && !tokenEncryptionKey.isBlank();
    }
}
