package com.utang.error;

/** Thrown when an action conflicts with current state (e.g. reminder already sent). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
