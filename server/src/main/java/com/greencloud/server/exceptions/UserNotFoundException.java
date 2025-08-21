package com.greencloud.server.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("user not found");
    }
}