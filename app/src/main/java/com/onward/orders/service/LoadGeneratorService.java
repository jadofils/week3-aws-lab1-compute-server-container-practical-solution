package com.onward.orders.service;

import com.onward.orders.dto.response.LoadStatusResponse;

/**
 * Generates CPU load on this server so Auto Scaling can be demonstrated.
 */
public interface LoadGeneratorService {
    /**
     * Starts a CPU spike for the given number of seconds, unless one is already running.
     *
     * @throws com.onward.orders.exception.InvalidLoadRequestException if seconds is outside the allowed range
     * @throws com.onward.orders.exception.LoadGeneratorDisabledException if the demo is switched off
     */
    LoadStatusResponse start(Integer seconds);

    LoadStatusResponse status();

    /** Stops a running spike early. */
    LoadStatusResponse stop();
}
