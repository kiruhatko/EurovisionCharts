package com.eurovision.analytics.telegram.command.admin;

import com.eurovision.analytics.chart.ChartPeriod;
import com.eurovision.analytics.chart.ChartService;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

@Component
public class YearCommand extends AbstractPeriodChartCommand {

    public YearCommand(ChartService chartService, MessageSender messageSender) {
        super(chartService, messageSender, ChartPeriod.YEAR);
    }

    @Override
    public String name() {
        return "year";
    }
}
