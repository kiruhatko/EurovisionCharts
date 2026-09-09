package com.eurovision.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(String botToken, String botUsername) {

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }
}
