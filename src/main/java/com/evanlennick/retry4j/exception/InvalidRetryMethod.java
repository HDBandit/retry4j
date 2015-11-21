package com.evanlennick.retry4j.exception;

public class InvalidRetryMethod extends Retry4jException {

    public InvalidRetryMethod(String message) {
        super(message);
    }
}
