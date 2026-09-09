package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import com.eurovision.analytics.user.UserService;
import org.springframework.stereotype.Component;

@Component
public class LogoutCommand implements Command {

    private final UserService userService;
    private final MessageSender messageSender;

    public LogoutCommand(UserService userService, MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "logout";
    }

    @Override
    public void handle(CommandContext ctx) {
        userService.logout(ctx.user());
        messageSender.send(ctx.chatId(),
                "Трекінг і участь у чарті вимкнено по всіх сервісах. Історичні дані збережено.");
    }
}
