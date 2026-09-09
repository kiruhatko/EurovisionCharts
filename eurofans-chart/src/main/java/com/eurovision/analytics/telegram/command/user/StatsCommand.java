package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.UserStatsService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class StatsCommand implements Command {

    private final UserStatsService userStatsService;
    private final MessageSender messageSender;

    public StatsCommand(UserStatsService userStatsService, MessageSender messageSender) {
        this.userStatsService = userStatsService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!ctx.user().isChartParticipationEnabled()) {
            messageSender.send(ctx.chatId(), "Участь у чарті вимкнена (/logout). Ваші дані не враховуються у рейтингу.");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>Ваша статистика</b>\n");
        for (ChartPeriod period : ChartPeriod.values()) {
            userStatsService.standingFor(ctx.user().getId(), period).ifPresentOrElse(
                    s -> sb.append(String.format("%s: %d прослуховувань, місце %d/%d (%.1f перцентиль)\n",
                            label(period), s.listenCount(), s.rank(), s.totalParticipants(), s.percentile())),
                    () -> sb.append(label(period)).append(": немає даних\n"));
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }

    private String label(ChartPeriod period) {
        return switch (period) {
            case WEEK -> "Тиждень";
            case MONTH -> "Місяць";
            case YEAR -> "Рік";
            case ALL_TIME -> "За весь час";
        };
    }
}
