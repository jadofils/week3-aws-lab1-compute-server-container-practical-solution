package com.onward.orders.exception;

/**
 * Base class for client errors that should be answered with HTTP 400 and a readable message.
 */
public abstract class BadRequestException extends RuntimeException {
    protected BadRequestException(String message) {
        super(message);
    }
}
