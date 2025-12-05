package com.example.subscription.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSubscriptionPlanRequest {
    
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency;
    
    private String name;
    
    private String description;
    
    private String features; // JSON string
}

