package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.ConnectedAccountService;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectionsCommand implements Command {

    private final ConnectedAccountService connectedAccountService;
    private final MessageSender messageSender;

    public ConnectionsCommand(ConnectedAccountService connectedAccountService, MessageSender messageSender) {
        this.connectedAccountService = connectedAccountService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "connections";
    }

    @Override
    public void handle(CommandContext ctx) {
        List<ConnectedAccount> accounts = connectedAccountService.findAllForUser(ctx.user().getId());
        Map<Provider, ConnectedAccount> byProvider = accounts.stream()
                .collect(Collectors.toMap(ConnectedAccount::getProvider, Function.identity(), (a, b) -> a));

        StringBuilder sb = new StringBuilder("<b>Ваші підключення</b>\n");
        for (Provider provider : Provider.values()) {
            ConnectedAccount account = byProvider.get(provider);
            if (account == null) {
                sb.append("• ").append(provider).append(": не підключено\n");
            } else {
                sb.append("• ").append(provider).append(": ").append(account.getStatus())
                        .append(" (").append(account.getProviderDisplayName()).append(")\n");
            }
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }
}
