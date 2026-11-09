package com.onward.orders.service;

import com.onward.orders.api.CreateOrderRequest;
import com.onward.orders.api.OrderView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private static final int MAX_RECENT_ORDERS = 50;
    private final ConcurrentLinkedDeque<OrderView> orders = new ConcurrentLinkedDeque<>();

    public OrderView create(CreateOrderRequest request, String serverId) {
        OrderView order = new OrderView(
                UUID.randomUUID(),
                request.customerName().trim(),
                request.item().trim(),
                request.quantity(),
                "RECEIVED",
                Instant.now(),
                serverId);
        orders.addFirst(order);
        while (orders.size() > MAX_RECENT_ORDERS) {
            orders.pollLast();
        }
        return order;
    }

    public List<OrderView> recentOrders() {
        return new ArrayList<>(orders);
    }
}
