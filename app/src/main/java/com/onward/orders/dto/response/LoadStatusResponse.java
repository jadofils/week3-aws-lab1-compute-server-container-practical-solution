package com.onward.orders.dto.response;

/**
 * State of the CPU spike on the server that answered.
 *
 * @param started true when this request started a new spike, false if one was already running
 */
public record LoadStatusResponse(
        boolean active,
        boolean started,
        long secondsRemaining,
        int workerThreads,
        String serverId,
        String availabilityZone) {
}
