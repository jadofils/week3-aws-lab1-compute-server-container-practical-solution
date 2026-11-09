package com.onward.orders.exception;

public class InvalidOrderException extends BadRequestException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
