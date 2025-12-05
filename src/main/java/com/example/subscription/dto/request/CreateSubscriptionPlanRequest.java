package com.example.subscription.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSubscriptionPlanRequest {
    
    @NotNull(message = "Subscription account ID is required")
    private Long subscriptionAccountId;
    
    @NotNull(message = "Duration type ID is required")
    private Long durationTypeId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency = "USD";
    
    private String name;
    
    private String description;
    
    private String features; // JSON string
}

