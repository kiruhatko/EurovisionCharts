package com.eurovision.analytics.chartstudio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chart-studio")
public record ChartStudioProperties(String username, String password) {
}
