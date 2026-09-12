package com.eurovision.analytics.chartstudio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth for everything under {@code /api/chart-studio/**} is enforced by
 * {@link ChartStudioSecurityConfig}, not here -- this controller only ever
 * needs to worry about the data.
 */
@RestController
public class ChartStudioController {

    private final ChartStudioService chartStudioService;

    public ChartStudioController(ChartStudioService chartStudioService) {
        this.chartStudioService = chartStudioService;
    }

    @GetMapping("/api/chart-studio/chart")
    public ChartStudioDtos.Response chart(@RequestParam(name = "weekOffset", defaultValue = "0") int weekOffset) {
        return chartStudioService.chartForWeek(Math.max(0, weekOffset));
    }
}
