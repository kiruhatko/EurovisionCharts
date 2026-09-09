package com.eurovision.analytics.telegram;

import com.eurovision.analytics.config.TelegramProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramClientConfig {

    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        // Constructed even when not configured (with an empty token) so that Spring wiring
        // never fails; TelegramBotLifecycle is what actually gates whether polling starts.
        return new OkHttpTelegramClient(properties.isConfigured() ? properties.botToken() : "unconfigured");
    }
}
