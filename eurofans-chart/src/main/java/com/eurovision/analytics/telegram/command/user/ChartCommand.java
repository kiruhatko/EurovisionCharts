package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.chart.ChartEntry;
import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChartCommand implements Command {

    private static final int TOP_N = 10;

    private final ChartService chartService;
    private final MessageSender messageSender;

    public ChartCommand(ChartService chartService, MessageSender messageSender) {
        this.chartService = chartService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "chart";
    }

    @Override
    public void handle(CommandContext ctx) {
        ChartPeriod period = parsePeriod(ctx.args());
        List<ChartEntry> chart = chartService.computeChart(period);
        if (chart.isEmpty()) {
            messageSender.send(ctx.chatId(), "Дані для чарту (" + period + ") ще відсутні.");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>🇪🇺 Eurovision Chart — " + period + "</b>\n\n");
        for (ChartEntry entry : chart.stream().limit(TOP_N).toList()) {
            sb.append(entry.rank()).append(". ").append(entry.artist().getCanonicalName())
                    .append(" — ").append(entry.listenCount()).append(" прослуховувань");
            if (entry.growthPercent() != null) {
                sb.append(" (").append(entry.growthPercent()).append("%)");
            }
            sb.append('\n');
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }

    static ChartPeriod parsePeriod(String[] args) {
        if (args.length == 0) {
            return ChartPeriod.WEEK;
        }
        return switch (args[0].toLowerCase()) {
            case "week" -> ChartPeriod.WEEK;
            case "month" -> ChartPeriod.MONTH;
            case "year" -> ChartPeriod.YEAR;
            case "alltime", "all_time", "all" -> ChartPeriod.ALL_TIME;
            default -> ChartPeriod.WEEK;
        };
    }
}
