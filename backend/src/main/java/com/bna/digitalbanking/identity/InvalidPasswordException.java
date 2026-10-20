package com.bna.digitalbanking.identity;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Password must contain no more than 72 UTF-8 bytes");
    }
}
