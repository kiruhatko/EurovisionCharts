package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.listening.ReprocessingService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class ReprocessCommand implements Command {

    private final ReprocessingService reprocessingService;
    private final MessageSender messageSender;

    public ReprocessCommand(ReprocessingService reprocessingService, MessageSender messageSender) {
        this.reprocessingService = reprocessingService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "reprocess";
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        messageSender.send(ctx.chatId(), "Переобробка історичних подій розпочата...");
        int changed = reprocessingService.reprocessUnresolved();
        messageSender.send(ctx.chatId(), "Переобробку завершено. Змінено записів: " + changed);
    }
}
