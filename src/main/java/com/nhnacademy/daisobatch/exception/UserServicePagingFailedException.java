package com.nhnacademy.daisobatch.exception;

public class UserServicePagingFailedException extends RuntimeException {
    public UserServicePagingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserServicePagingFailedException(String message) {
        super(message);
    }
}
