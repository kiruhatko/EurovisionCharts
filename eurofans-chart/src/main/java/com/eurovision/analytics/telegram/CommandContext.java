package com.eurovision.analytics.telegram;

import com.eurovision.analytics.user.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/** Everything a command handler needs, already validated (group binding, admin check where required). */
public record CommandContext(
        Message message,
        long chatId,
        long telegramUserId,
        String telegramUsername,
        String firstName,
        String argsRaw,
        User user
) {

    public String[] args() {
        if (argsRaw == null || argsRaw.isBlank()) {
            return new String[0];
        }
        return argsRaw.trim().split("\\s+");
    }
}
