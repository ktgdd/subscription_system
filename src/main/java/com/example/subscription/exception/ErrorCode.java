package com.example.subscription.exception;

public enum ErrorCode {
    
    VALIDATION_ERROR("VALIDATION_ERROR", "Invalid request parameters"),
    DUPLICATE_REQUEST("DUPLICATE_REQUEST", "Duplicate request detected. Please wait before retrying"),
    SUBSCRIPTION_NOT_FOUND("SUBSCRIPTION_NOT_FOUND", "Subscription not found"),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized access"),
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}

