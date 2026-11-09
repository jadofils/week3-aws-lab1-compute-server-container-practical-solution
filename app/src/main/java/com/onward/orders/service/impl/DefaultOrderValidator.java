package com.onward.orders.service.impl;

import com.onward.orders.config.OrderProperties;
import com.onward.orders.dto.request.CreateOrderRequest;
import com.onward.orders.exception.InvalidOrderException;
import com.onward.orders.service.OrderValidator;
import org.springframework.stereotype.Component;

@Component
public class DefaultOrderValidator implements OrderValidator {
    private final OrderProperties properties;

    public DefaultOrderValidator(OrderProperties properties) {
        this.properties = properties;
    }

    @Override
    public void validate(CreateOrderRequest request) {
        if (request == null || isBlank(request.customerName()) || isBlank(request.item())) {
            throw new InvalidOrderException("Customer name and item are required.");
        }
        Integer quantity = request.quantity();
        if (quantity == null || quantity < properties.minQuantity() || quantity > properties.maxQuantity()) {
            throw new InvalidOrderException("Quantity must be between %d and %d."
                    .formatted(properties.minQuantity(), properties.maxQuantity()));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
