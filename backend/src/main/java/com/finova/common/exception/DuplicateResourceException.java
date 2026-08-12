package com.finova.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when creating something that violates a uniqueness constraint. Maps to HTTP 409. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
