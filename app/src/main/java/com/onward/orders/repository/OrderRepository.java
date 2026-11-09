package com.onward.orders.repository;

import com.onward.orders.model.Order;
import java.util.List;

/**
 * Storage abstraction for orders. A database-backed implementation can replace the
 * in-memory one without changing the services.
 */
public interface OrderRepository {
    Order save(Order order);

    List<Order> findRecent();
}
