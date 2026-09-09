package com.eurovision.analytics.telegram;

import com.eurovision.analytics.config.TelegramProperties;
import com.eurovision.analytics.deployment.DeploymentBindingVerifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Starts long-polling only after {@link DeploymentBindingVerifier} has
 * already run (a hard constructor dependency, so Spring cannot construct
 * this bean until that one's {@code @PostConstruct} has either succeeded or
 * thrown and aborted the whole application context) and only when a bot
 * token is actually configured.
 */
@Component
public class TelegramBotLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotLifecycle.class);

    private final TelegramProperties properties;
    private final UpdateDispatcher updateDispatcher;
    private final DeploymentBindingVerifier deploymentBindingVerifier;

    private TelegramBotsLongPollingApplication application;

    public TelegramBotLifecycle(TelegramProperties properties,
                                 UpdateDispatcher updateDispatcher,
                                 DeploymentBindingVerifier deploymentBindingVerifier) {
        this.properties = properties;
        this.updateDispatcher = updateDispatcher;
        this.deploymentBindingVerifier = deploymentBindingVerifier;
    }

    @PostConstruct
    void start() {
        deploymentBindingVerifier.requireVerified(); // fail loudly if this bean somehow started out of order

        if (!properties.isConfigured()) {
            log.warn("TELEGRAM_BOT_TOKEN is not configured; the Telegram bot will not start.");
            return;
        }
        try {
            application = new TelegramBotsLongPollingApplication();
            application.registerBot(properties.botToken(), updateDispatcher);
            log.info("Telegram bot long-polling started.");
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Failed to start Telegram bot long-polling", e);
        }
    }

    @PreDestroy
    void stop() {
        if (application != null) {
            try {
                application.close();
            } catch (Exception e) {
                log.warn("Error stopping Telegram bot: {}", e.getMessage());
            }
        }
    }
}
