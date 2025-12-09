package com.example.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtendSubscriptionRequest {
    
    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;
}

