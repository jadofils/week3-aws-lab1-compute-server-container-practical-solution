package com.onward.orders.service.impl;

import com.onward.orders.dto.request.CreateOrderRequest;
import com.onward.orders.dto.response.OrderResponse;
import com.onward.orders.mapper.OrderMapper;
import com.onward.orders.model.Order;
import com.onward.orders.model.OrderStatus;
import com.onward.orders.repository.OrderRepository;
import com.onward.orders.service.OrderService;
import com.onward.orders.service.OrderValidator;
import com.onward.orders.service.ServerIdentityService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repository;
    private final OrderValidator validator;
    private final OrderMapper mapper;
    private final ServerIdentityService serverIdentity;

    public OrderServiceImpl(OrderRepository repository,
                            OrderValidator validator,
                            OrderMapper mapper,
                            ServerIdentityService serverIdentity) {
        this.repository = repository;
        this.validator = validator;
        this.mapper = mapper;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public OrderResponse create(CreateOrderRequest request) {
        validator.validate(request);
        Order order = new Order(
                UUID.randomUUID(),
                request.customerName().trim(),
                request.item().trim(),
                request.quantity(),
                OrderStatus.RECEIVED,
                Instant.now(),
                serverIdentity.current().label());
        return mapper.toResponse(repository.save(order));
    }

    @Override
    public List<OrderResponse> recentOrders() {
        return repository.findRecent().stream().map(mapper::toResponse).toList();
    }
}
