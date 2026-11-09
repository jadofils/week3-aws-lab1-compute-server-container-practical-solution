package com.onward.orders.api;

public record CreateOrderRequest(String customerName, String item, int quantity) {
}
