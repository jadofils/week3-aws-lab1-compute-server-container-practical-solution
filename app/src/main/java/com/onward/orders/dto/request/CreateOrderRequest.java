package com.onward.orders.dto.request;

public record CreateOrderRequest(String customerName, String item, Integer quantity) {
}
