package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class MonthCommand extends AbstractPeriodChartCommand {

    public MonthCommand(ChartService chartService, MessageSender messageSender) {
        super(chartService, messageSender, ChartPeriod.MONTH);
    }

    @Override
    public String name() {
        return "month";
    }
}
