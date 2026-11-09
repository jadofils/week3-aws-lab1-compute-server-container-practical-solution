package com.onward.orders.controller;

import com.onward.orders.dto.response.HealthResponse;
import com.onward.orders.service.ServerIdentityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Load balancer target-group health check.
 */
@RestController
public class HealthController {
    private final ServerIdentityService serverIdentity;

    public HealthController(ServerIdentityService serverIdentity) {
        this.serverIdentity = serverIdentity;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", serverIdentity.currentServer());
    }
}
