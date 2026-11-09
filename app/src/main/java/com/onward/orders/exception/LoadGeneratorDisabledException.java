package com.onward.orders.exception;

public class LoadGeneratorDisabledException extends RuntimeException {
    public LoadGeneratorDisabledException() {
        super("The load spike demo is disabled on this server.");
    }
}
