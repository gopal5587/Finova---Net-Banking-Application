package com.finova.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain exceptions that map cleanly to HTTP responses.
 * Carrying the status here keeps the controller advice simple and consistent.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
