package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.UserChartStanding;
import com.eurovision.analytics.chart.UserStatsService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MeCommand implements Command {

    private final UserStatsService userStatsService;
    private final MessageSender messageSender;

    public MeCommand(UserStatsService userStatsService, MessageSender messageSender) {
        this.userStatsService = userStatsService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "me";
    }

    @Override
    public void handle(CommandContext ctx) {
        if (!ctx.user().isChartParticipationEnabled()) {
            messageSender.send(ctx.chatId(), "Участь у чарті вимкнена (/logout). Ваші дані не враховуються у рейтингу.");
            return;
        }
        Optional<UserChartStanding> standing = userStatsService.standingFor(ctx.user().getId(), ChartPeriod.WEEK);
        if (standing.isEmpty()) {
            messageSender.send(ctx.chatId(), "За цей тиждень ще немає жодного зафіксованого Eurovision-прослуховування.");
            return;
        }
        UserChartStanding s = standing.get();
        messageSender.send(ctx.chatId(), String.format(
                "<b>Ваша статистика (тиждень)</b>\nПрослуховувань: %d\nМісце: %d з %d\nПерцентиль: %.1f%%",
                s.listenCount(), s.rank(), s.totalParticipants(), s.percentile()));
    }
}
