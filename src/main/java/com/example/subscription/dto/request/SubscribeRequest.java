package com.example.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {
    
    @NotNull(message = "Subscription plan ID is required")
    private Long subscriptionPlanId;
}

