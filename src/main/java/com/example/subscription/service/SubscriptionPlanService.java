package com.example.subscription.service;

import com.example.subscription.model.SubscriptionPlan;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanService {
    
    List<SubscriptionPlan> getActivePlansByAccount(Long accountId);
    
    Optional<SubscriptionPlan> getPlanById(Long planId);
    
    SubscriptionPlan createPlan(SubscriptionPlan plan);
    
    SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updatedPlan);
    
    void deletePlan(Long planId);
}
