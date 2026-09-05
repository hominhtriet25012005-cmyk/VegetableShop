package com.vegetableshop.exception;

public class InvalidAccountActivationTokenException extends RuntimeException {

    public InvalidAccountActivationTokenException(String message) {
        super(message);
    }
}
