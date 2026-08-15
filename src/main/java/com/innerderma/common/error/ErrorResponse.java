package com.innerderma.common.error;

import java.util.Map;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> errors
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.code(), errorCode.message(), Map.of());
    }

    public static ErrorResponse validation(Map<String, String> errors) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return new ErrorResponse(false, errorCode.code(), errorCode.message(), errors);
    }
}
