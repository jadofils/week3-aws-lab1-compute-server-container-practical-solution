package com.onward.orders.exception;

public class InvalidLoadRequestException extends BadRequestException {
    public InvalidLoadRequestException(String message) {
        super(message);
    }
}
