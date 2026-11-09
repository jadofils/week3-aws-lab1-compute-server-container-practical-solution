package com.onward.orders.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerName,
        String item,
        int quantity,
        String status,
        Instant receivedAt,
        String servedBy) {
}
