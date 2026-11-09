package com.onward.orders.service.impl;

import com.onward.orders.config.LoadProperties;
import com.onward.orders.dto.response.LoadStatusResponse;
import com.onward.orders.exception.InvalidLoadRequestException;
import com.onward.orders.exception.LoadGeneratorDisabledException;
import com.onward.orders.model.ServerIdentity;
import com.onward.orders.service.LoadGeneratorService;
import com.onward.orders.service.ServerIdentityService;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps every CPU core of this server busy until a deadline, which raises the Auto Scaling
 * group's average CPU and triggers the target-tracking policy. Only one spike runs per server,
 * and its length is bounded by {@link LoadProperties}.
 */
@Service
public class CpuSpikeLoadGeneratorService implements LoadGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(CpuSpikeLoadGeneratorService.class);

    private final LoadProperties properties;
    private final ServerIdentityService serverIdentity;
    private final int workerThreads = Runtime.getRuntime().availableProcessors();
    private final AtomicReference<Instant> deadline = new AtomicReference<>(Instant.EPOCH);
    private final ExecutorService workers = Executors.newFixedThreadPool(workerThreads, runnable -> {
        Thread thread = new Thread(runnable, "cpu-spike");
        thread.setDaemon(true);
        return thread;
    });

    public CpuSpikeLoadGeneratorService(LoadProperties properties, ServerIdentityService serverIdentity) {
        this.properties = properties;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public LoadStatusResponse start(Integer seconds) {
        if (!properties.enabled()) {
            throw new LoadGeneratorDisabledException();
        }
        if (seconds == null || seconds < properties.minSeconds() || seconds > properties.maxSeconds()) {
            throw new InvalidLoadRequestException("Spike length must be between %d and %d seconds."
                    .formatted(properties.minSeconds(), properties.maxSeconds()));
        }

        Instant now = Instant.now();
        Instant current = deadline.get();
        if (current.isAfter(now)) {
            return snapshot(false);
        }
        Instant newDeadline = now.plusSeconds(seconds);
        if (!deadline.compareAndSet(current, newDeadline)) {
            return snapshot(false);
        }

        log.info("Starting CPU spike for {}s on {} threads", seconds, workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            workers.submit(() -> burnUntil(newDeadline));
        }
        return snapshot(true);
    }

    @Override
    public LoadStatusResponse status() {
        return snapshot(false);
    }

    @Override
    public LoadStatusResponse stop() {
        deadline.set(Instant.EPOCH);
        log.info("CPU spike stopped");
        return snapshot(false);
    }

    private void burnUntil(Instant until) {
        double sink = 0;
        // Stop at the deadline, or earlier if stop() moved the deadline.
        while (Instant.now().isBefore(until) && deadline.get().equals(until)) {
            for (int i = 1; i < 50_000; i++) {
                sink += Math.sqrt(i) * Math.sin(i);
            }
        }
        if (sink == 42) {
            log.trace("unreachable; keeps the loop from being optimised away");
        }
    }

    private LoadStatusResponse snapshot(boolean started) {
        long remaining = Math.max(0, Duration.between(Instant.now(), deadline.get()).toSeconds());
        ServerIdentity identity = serverIdentity.current();
        return new LoadStatusResponse(remaining > 0, started, remaining, workerThreads,
                identity.serverId(), identity.availabilityZone());
    }

    @PreDestroy
    void shutdown() {
        deadline.set(Instant.EPOCH);
        workers.shutdownNow();
    }
}
