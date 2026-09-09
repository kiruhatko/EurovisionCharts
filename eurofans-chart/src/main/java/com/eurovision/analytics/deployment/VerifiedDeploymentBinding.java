package com.eurovision.analytics.deployment;

/**
 * Outcome of a successful boot-time binding verification. The only place in
 * the application allowed to answer "what group is this bot allowed to act
 * in". Never mutated after construction, never exposed to any Telegram
 * command.
 */
public record VerifiedDeploymentBinding(
        boolean casualMode,
        Long authorizedChatId,
        String deploymentId,
        String environment
) {

    public boolean isChatAuthorized(long chatId) {
        if (casualMode) {
            // Casual mode intentionally has no group lock (single-user personal use).
            return true;
        }
        return authorizedChatId != null && authorizedChatId == chatId;
    }
}
