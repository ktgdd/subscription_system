package com.example.subscription.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionException(SubscriptionException e) {
        HttpStatus status = mapErrorCodeToHttpStatus(e.getErrorCode());
        String message = e.getCustomMessage() != null ? e.getCustomMessage() : e.getErrorCode().getMessage();
        
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getErrorCode().getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                    ErrorCode.INTERNAL_ERROR.getCode(), 
                    ErrorCode.INTERNAL_ERROR.getMessage()
                ));
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case DUPLICATE_REQUEST -> HttpStatus.CONFLICT;
            case SUBSCRIPTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    public record ErrorResponse(String code, String message) {}
}

