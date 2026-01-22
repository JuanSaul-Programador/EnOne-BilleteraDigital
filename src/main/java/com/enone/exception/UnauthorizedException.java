package com.enone.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("No autorizado");
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}