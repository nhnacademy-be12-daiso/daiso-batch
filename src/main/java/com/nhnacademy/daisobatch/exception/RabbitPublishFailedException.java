package com.nhnacademy.daisobatch.exception;

public class RabbitPublishFailedException extends RuntimeException {
    public RabbitPublishFailedException(String message) { super(message); }
    public RabbitPublishFailedException(String message, Throwable cause) { super(message, cause); }
}

