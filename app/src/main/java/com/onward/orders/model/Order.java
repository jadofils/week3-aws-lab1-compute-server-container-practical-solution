package com.onward.orders.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for an order. Independent of the HTTP layer.
 */
public record Order(
        UUID id,
        String customerName,
        String item,
        int quantity,
        OrderStatus status,
        Instant receivedAt,
        String servedBy) {
}
