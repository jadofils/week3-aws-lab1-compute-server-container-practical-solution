package com.onward.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Display metadata for the demo, bound from {@code app.info.*} in application.properties.
 */
@ConfigurationProperties(prefix = "app.info")
public record AppInfoProperties(String studentName, String labName, String applicationName) {
}
