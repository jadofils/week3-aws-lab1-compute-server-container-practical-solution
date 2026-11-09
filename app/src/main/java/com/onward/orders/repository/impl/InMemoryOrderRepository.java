package com.onward.orders.repository.impl;

import com.onward.orders.config.OrderProperties;
import com.onward.orders.model.Order;
import com.onward.orders.repository.OrderRepository;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Repository;

/**
 * Keeps the most recent orders in memory on this server only. Data is lost on restart.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentLinkedDeque<Order> orders = new ConcurrentLinkedDeque<>();
    private final int maxRecentOrders;

    public InMemoryOrderRepository(OrderProperties properties) {
        this.maxRecentOrders = properties.maxRecentOrders();
    }

    @Override
    public Order save(Order order) {
        orders.addFirst(order);
        while (orders.size() > maxRecentOrders) {
            orders.pollLast();
        }
        return order;
    }

    @Override
    public List<Order> findRecent() {
        return List.copyOf(orders);
    }
}
