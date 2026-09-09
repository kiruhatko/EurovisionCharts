package com.eurovision.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Admin identity is exclusively a numeric Telegram user ID allowlist.
 * Never a username, never a group membership.
 */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String telegramIds) {

    public Set<Long> ids() {
        if (telegramIds == null || telegramIds.isBlank()) {
            return Set.of();
        }
        return Stream.of(telegramIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(long telegramUserId) {
        return ids().contains(telegramUserId);
    }
}
