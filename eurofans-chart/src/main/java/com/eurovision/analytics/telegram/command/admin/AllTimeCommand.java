package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class AllTimeCommand extends AbstractPeriodChartCommand {

    public AllTimeCommand(ChartService chartService, MessageSender messageSender) {
        super(chartService, messageSender, ChartPeriod.ALL_TIME);
    }

    @Override
    public String name() {
        return "alltime";
    }
}
