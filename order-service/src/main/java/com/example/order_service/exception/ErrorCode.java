package com.example.order_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    BAD_REQUEST("Bad request", HttpStatus.BAD_REQUEST),
    EMPTY_CART("Cart is empty", HttpStatus.BAD_REQUEST),
    INVALID_STATUS("Invalid order status transition", HttpStatus.BAD_REQUEST),
    PAYMENT_REQUIRED("Order must be paid before shipping or completion", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Forbidden", HttpStatus.FORBIDDEN),
    SERVICE_UNAVAILABLE("Downstream service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}
