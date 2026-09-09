package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.chart.ChartEntry;
import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;

import java.util.List;

/** Admin period views show the full ranked list (no top-N truncation), unlike the user-facing {@code /chart}. */
public abstract class AbstractPeriodChartCommand implements Command {

    private final ChartService chartService;
    private final MessageSender messageSender;
    private final ChartPeriod period;

    protected AbstractPeriodChartCommand(ChartService chartService, MessageSender messageSender, ChartPeriod period) {
        this.chartService = chartService;
        this.messageSender = messageSender;
        this.period = period;
    }

    @Override
    public boolean requiresAdmin() {
        return true;
    }

    @Override
    public void handle(CommandContext ctx) {
        List<ChartEntry> chart = chartService.computeChart(period);
        if (chart.isEmpty()) {
            messageSender.send(ctx.chatId(), "Дані для чарту (" + period + ") ще відсутні.");
            return;
        }
        StringBuilder sb = new StringBuilder("<b>Chart — " + period + "</b>\n\n");
        for (ChartEntry entry : chart) {
            sb.append(entry.rank()).append(". ").append(entry.artist().getCanonicalName())
                    .append(" — ").append(entry.listenCount())
                    .append(" (").append(entry.uniqueListeners()).append(" слухачів)\n");
        }
        messageSender.send(ctx.chatId(), sb.toString());
    }
}
