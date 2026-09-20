package com.ewb.payment.dto;

import java.time.Instant;

public record ErrorResponse(String errorCode, String message, Instant timestamp) {

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, Instant.now());
    }
}