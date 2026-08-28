package com.vegetableshop.exception;

public class OrderOperationException extends RuntimeException {
    public OrderOperationException(String message) {
        super(message);
    }
}
