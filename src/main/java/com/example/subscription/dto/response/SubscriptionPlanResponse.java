package com.example.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {
    
    private Long id;
    private Long subscriptionAccountId;
    private String accountName;
    private Long durationTypeId;
    private String durationType;
    private Integer durationDays;
    private BigDecimal amount;
    private String currency;
    private String name;
    private String description;
    private String features;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

