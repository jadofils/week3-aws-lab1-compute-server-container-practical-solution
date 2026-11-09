package com.onward.orders.service;

import com.onward.orders.dto.request.CreateOrderRequest;

public interface OrderValidator {
    /**
     * @throws com.onward.orders.exception.InvalidOrderException if the request breaks an order rule
     */
    void validate(CreateOrderRequest request);
}
