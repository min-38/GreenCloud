package com.greencloud.server.exceptions;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("invalid token");
    }
}
