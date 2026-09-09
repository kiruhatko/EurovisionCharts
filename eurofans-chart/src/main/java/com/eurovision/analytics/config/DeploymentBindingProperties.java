package com.eurovision.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deployment")
public record DeploymentBindingProperties(
        boolean bindingEnabled,
        String authorizedGroupChatId,
        String bindingFilePath,
        String signingPublicKey,
        String applicationIdentifier,
        String environment
) {

    public Long authorizedGroupChatIdAsLong() {
        if (authorizedGroupChatId == null || authorizedGroupChatId.isBlank()) {
            return null;
        }
        return Long.parseLong(authorizedGroupChatId.trim());
    }
}
