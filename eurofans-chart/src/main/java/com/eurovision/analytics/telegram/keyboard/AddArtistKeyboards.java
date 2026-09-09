package com.eurovision.analytics.telegram.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

public final class AddArtistKeyboards {

    private AddArtistKeyboards() {
    }

    public static final String CONFIRM_PREFIX = "addartist:confirm:";
    public static final String CANCEL_PREFIX = "addartist:cancel:";

    public static InlineKeyboardMarkup confirmCancel(long artistId) {
        InlineKeyboardButton confirm = InlineKeyboardButton.builder()
                .text("✅ Confirm")
                .callbackData(CONFIRM_PREFIX + artistId)
                .build();
        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text("❌ Cancel")
                .callbackData(CANCEL_PREFIX + artistId)
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(confirm, cancel)))
                .build();
    }
}
