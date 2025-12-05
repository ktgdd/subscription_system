package com.example.subscription.util;

import com.example.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.example.subscription.dto.response.SubscriptionPlanResponse;
import com.example.subscription.model.SubscriptionAccount;
import com.example.subscription.model.DurationType;
import com.example.subscription.model.SubscriptionPlan;

public class SubscriptionPlanMapper {

    public static SubscriptionPlan toEntity(CreateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setSubscriptionAccountId(request.getSubscriptionAccountId());
        plan.setDurationTypeId(request.getDurationTypeId());
        plan.setAmount(request.getAmount());
        plan.setCurrency(request.getCurrency());
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setFeatures(request.getFeatures());
        plan.setIsActive(true);
        return plan;
    }

    public static SubscriptionPlanResponse toResponse(SubscriptionPlan plan, SubscriptionAccount account, DurationType durationType) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .subscriptionAccountId(plan.getSubscriptionAccountId())
                .accountName(account != null ? account.getName() : null)
                .durationTypeId(plan.getDurationTypeId())
                .durationType(durationType != null ? durationType.getType() : null)
                .durationDays(durationType != null ? durationType.getDays() : null)
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .name(plan.getName())
                .description(plan.getDescription())
                .features(plan.getFeatures())
                .isActive(plan.getIsActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}

