package com.onward.orders.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onward.orders.config.OrderProperties;
import com.onward.orders.dto.request.CreateOrderRequest;
import com.onward.orders.exception.InvalidOrderException;
import org.junit.jupiter.api.Test;

class DefaultOrderValidatorTest {
    private final DefaultOrderValidator validator = new DefaultOrderValidator(new OrderProperties(50, 1, 100));

    @Test
    void acceptsValidOrder() {
        assertThatCode(() -> validator.validate(new CreateOrderRequest("Ana", "Parcel", 3)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void rejectsBlankCustomerOrItem() {
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest(" ", "Parcel", 1)))
                .hasMessage("Customer name and item are required.");
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest("Ana", null, 1)))
                .hasMessage("Customer name and item are required.");
    }

    @Test
    void rejectsQuantityOutsideLimits() {
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest("Ana", "Parcel", 0)))
                .hasMessage("Quantity must be between 1 and 100.");
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest("Ana", "Parcel", 101)))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> validator.validate(new CreateOrderRequest("Ana", "Parcel", null)))
                .isInstanceOf(InvalidOrderException.class);
    }
}
