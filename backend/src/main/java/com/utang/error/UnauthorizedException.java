package com.utang.error;

/** Thrown when authentication is missing or invalid. Maps to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
