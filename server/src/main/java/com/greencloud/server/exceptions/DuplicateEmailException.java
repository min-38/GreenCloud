package com.greencloud.server.exceptions;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("email already used");
    }
}
