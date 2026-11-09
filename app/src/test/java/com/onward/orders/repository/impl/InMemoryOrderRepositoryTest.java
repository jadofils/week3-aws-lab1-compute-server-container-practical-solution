package com.onward.orders.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.onward.orders.config.OrderProperties;
import com.onward.orders.model.Order;
import com.onward.orders.model.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    @Test
    void keepsNewestFirstAndCapsAtConfiguredLimit() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository(new OrderProperties(2, 1, 100));

        Order first = order("first");
        Order second = order("second");
        Order third = order("third");
        repository.save(first);
        repository.save(second);
        repository.save(third);

        assertThat(repository.findRecent()).containsExactly(third, second);
    }

    private static Order order(String customer) {
        return new Order(UUID.randomUUID(), customer, "Parcel", 1, OrderStatus.RECEIVED, Instant.now(), "test");
    }
}
