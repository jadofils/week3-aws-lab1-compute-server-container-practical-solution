package com.onward.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limits for the sales-day CPU spike demo, bound from {@code app.load.*}.
 * The limits make the trigger safe to expose: it cannot run forever, cannot be stacked,
 * and can be switched off entirely.
 */
@ConfigurationProperties(prefix = "app.load")
public record LoadProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30") int minSeconds,
        @DefaultValue("600") int maxSeconds) {
}
