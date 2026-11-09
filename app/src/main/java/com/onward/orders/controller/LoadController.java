package com.onward.orders.controller;

import com.onward.orders.dto.request.LoadSpikeRequest;
import com.onward.orders.dto.response.LoadStatusResponse;
import com.onward.orders.service.LoadGeneratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sales-day simulator: starts a bounded CPU spike on the server that answers the request.
 * Each POST lands on one server; the UI sends several so the load balancer spreads them.
 */
@RestController
@RequestMapping("/api/load")
public class LoadController {
    private final LoadGeneratorService loadGenerator;

    public LoadController(LoadGeneratorService loadGenerator) {
        this.loadGenerator = loadGenerator;
    }

    @PostMapping("/spike")
    public LoadStatusResponse startSpike(@RequestBody LoadSpikeRequest request) {
        return loadGenerator.start(request == null ? null : request.seconds());
    }

    @PostMapping("/stop")
    public LoadStatusResponse stop() {
        return loadGenerator.stop();
    }

    @GetMapping("/status")
    public LoadStatusResponse status() {
        return loadGenerator.status();
    }
}
