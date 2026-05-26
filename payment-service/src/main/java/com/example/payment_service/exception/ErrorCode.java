package com.example.payment_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND("Payment not found", HttpStatus.NOT_FOUND),
    BAD_REQUEST("Bad request", HttpStatus.BAD_REQUEST),
    DUPLICATE_PAYMENT("Active payment already exists for this order", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_STATE("Invalid payment state for this operation", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD("Invalid payment method for this operation", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Forbidden", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}
