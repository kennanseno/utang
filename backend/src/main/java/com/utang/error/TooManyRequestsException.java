package com.utang.error;

/** Thrown when a client exceeds an allowed request rate (maps to HTTP 429). */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
