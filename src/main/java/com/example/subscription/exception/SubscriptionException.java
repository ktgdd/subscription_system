package com.example.subscription.exception;

public class SubscriptionException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String customMessage;
    
    public SubscriptionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }
    
    public SubscriptionException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
    
    public SubscriptionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.customMessage = null;
    }
    
    public SubscriptionException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage != null ? customMessage : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getCustomMessage() {
        return customMessage;
    }
}

