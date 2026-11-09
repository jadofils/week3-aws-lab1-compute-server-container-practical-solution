package com.onward.orders.mapper;

import com.onward.orders.dto.response.OrderResponse;
import com.onward.orders.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.id(),
                order.customerName(),
                order.item(),
                order.quantity(),
                order.status().name(),
                order.receivedAt(),
                order.servedBy());
    }
}
