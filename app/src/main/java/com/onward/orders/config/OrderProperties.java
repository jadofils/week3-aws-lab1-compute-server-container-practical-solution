package com.onward.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Order rules, bound from {@code app.orders.*} in application.properties.
 */
@ConfigurationProperties(prefix = "app.orders")
public record OrderProperties(
        @DefaultValue("50") int maxRecentOrders,
        @DefaultValue("1") int minQuantity,
        @DefaultValue("100") int maxQuantity) {
}
