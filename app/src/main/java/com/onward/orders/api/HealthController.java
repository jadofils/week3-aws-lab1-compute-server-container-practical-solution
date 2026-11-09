package com.onward.orders.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final ServerIdentity serverIdentity;

    public HealthController(ServerIdentity serverIdentity) {
        this.serverIdentity = serverIdentity;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "server", serverIdentity.currentServer());
    }
}
