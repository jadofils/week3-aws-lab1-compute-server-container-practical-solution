package com.onward.orders.api;

import com.onward.orders.service.OrderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final OrderService orderService;
    private final ServerIdentity serverIdentity;

    public ApiController(OrderService orderService, ServerIdentity serverIdentity) {
        this.orderService = orderService;
        this.serverIdentity = serverIdentity;
    }

    @GetMapping("/info")
    public ServerInfo info() {
        return new ServerInfo(
                "Jado Fils SEZIKEYE",
                "Lab 1 - Handling Sales Day Demand",
                "Onward Logistics Order System",
                serverIdentity.currentServer());
    }

    @GetMapping("/orders")
    public List<OrderView> listOrders() {
        return orderService.recentOrders();
    }

    @PostMapping("/orders")
    public OrderView createOrder(@RequestBody CreateOrderRequest request) {
        if (request == null || isBlank(request.customerName()) || isBlank(request.item())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Customer name and item are required.");
        }
        if (request.quantity() < 1 || request.quantity() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quantity must be between 1 and 100.");
        }
        return orderService.create(request, serverIdentity.currentServer());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
