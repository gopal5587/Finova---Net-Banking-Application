package com.finova.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation is syntactically valid but violates a business rule
 * (e.g. insufficient funds, frozen account). Maps to HTTP 422.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
