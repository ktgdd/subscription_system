package com.example.subscription.util;

import com.example.subscription.dto.response.UserSubscriptionResponse;
import com.example.subscription.model.SubscriptionAccount;
import com.example.subscription.model.DurationType;
import com.example.subscription.model.UserSubscription;

import java.time.LocalDate;

public class UserSubscriptionMapper {

    public static UserSubscriptionResponse toResponse(UserSubscription subscription, SubscriptionAccount account, DurationType durationType) {
        long daysRemaining = 0;
        if (subscription.getEndDate() != null && subscription.getStatus().equals("ACTIVE")) {
            LocalDate today = LocalDate.now();
            if (subscription.getEndDate().isAfter(today)) {
                daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, subscription.getEndDate());
            }
        }

        return UserSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .subscriptionAccountId(subscription.getSubscriptionAccountId())
                .accountName(account != null ? account.getName() : null)
                .durationTypeId(subscription.getDurationTypeId())
                .durationType(durationType != null ? durationType.getType() : null)
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .daysRemaining(daysRemaining)
                .createdAt(subscription.getCreatedAt())
                .lastUpdatedAt(subscription.getLastUpdatedAt())
                .build();
    }
}

