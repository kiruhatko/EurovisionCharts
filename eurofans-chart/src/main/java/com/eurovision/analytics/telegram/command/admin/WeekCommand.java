package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class WeekCommand extends AbstractPeriodChartCommand {

    public WeekCommand(ChartService chartService, MessageSender messageSender) {
        super(chartService, messageSender, ChartPeriod.WEEK);
    }

    @Override
    public String name() {
        return "week";
    }
}
