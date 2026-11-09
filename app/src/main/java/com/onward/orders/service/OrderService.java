package com.onward.orders.service;

import com.onward.orders.dto.request.CreateOrderRequest;
import com.onward.orders.dto.response.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse create(CreateOrderRequest request);

    List<OrderResponse> recentOrders();
}
