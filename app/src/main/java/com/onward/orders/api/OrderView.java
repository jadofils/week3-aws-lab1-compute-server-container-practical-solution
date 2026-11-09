package com.onward.orders.api;

import java.time.Instant;
import java.util.UUID;

public record OrderView(
        UUID id,
        String customerName,
        String item,
        int quantity,
        String status,
        Instant receivedAt,
        String servedBy) {
}
