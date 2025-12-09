package com.example.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponse {
    
    private Long id;
    private Long userId;
    private Long subscriptionAccountId;
    private String accountName;
    private Long durationTypeId;
    private String durationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long daysRemaining;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
}

