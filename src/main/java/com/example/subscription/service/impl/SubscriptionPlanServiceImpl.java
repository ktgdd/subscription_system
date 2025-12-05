package com.example.subscription.service.impl;

import com.example.subscription.cache.SubscriptionPlanCache;
import com.example.subscription.model.SubscriptionPlan;
import com.example.subscription.repository.SubscriptionPlanRepository;
import com.example.subscription.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanCache subscriptionPlanCache;

    @Override
    public List<SubscriptionPlan> getActivePlansByAccount(Long accountId) {
        // Try cache first
        return subscriptionPlanCache.getAccountPlans(accountId)
                .blockOptional()
                .orElseGet(() -> {
                    // Cache miss - fetch from database
                    List<SubscriptionPlan> plans = subscriptionPlanRepository
                            .findBySubscriptionAccountIdAndIsActiveTrueAndDeletedAtIsNull(accountId);
                    
                    // Cache for future requests
                    subscriptionPlanCache.cacheAccountPlans(accountId, plans).subscribe();
                    
                    return plans;
                });
    }

    @Override
    public Optional<SubscriptionPlan> getPlanById(Long planId) {
        // Try cache first
        return subscriptionPlanCache.getPlan(planId)
                .blockOptional()
                .or(() -> {
                    // Cache miss - fetch from database
                    Optional<SubscriptionPlan> plan = subscriptionPlanRepository.findById(planId);
                    plan.ifPresent(p -> subscriptionPlanCache.cachePlan(p).subscribe());
                    return plan;
                });
    }

    @Override
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        // Deactivate old plan if exists (business rule: only one active per account+duration_type)
        subscriptionPlanRepository.findActivePlanByAccountAndDurationType(
                plan.getSubscriptionAccountId(), 
                plan.getDurationTypeId()
        ).ifPresent(oldPlan -> {
            oldPlan.setIsActive(false);
            oldPlan.setDeletedAt(java.time.LocalDateTime.now());
            subscriptionPlanRepository.save(oldPlan);
            subscriptionPlanCache.invalidatePlan(oldPlan.getId()).subscribe();
        });

        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        
        // Cache the new plan
        subscriptionPlanCache.cachePlan(saved).subscribe();
        subscriptionPlanCache.invalidateAccountPlans(saved.getSubscriptionAccountId()).subscribe();
        
        log.info("Created subscription plan: id={}, accountId={}", saved.getId(), saved.getSubscriptionAccountId());
        return saved;
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updatedPlan) {
        SubscriptionPlan existing = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        existing.setAmount(updatedPlan.getAmount());
        existing.setCurrency(updatedPlan.getCurrency());
        existing.setName(updatedPlan.getName());
        existing.setDescription(updatedPlan.getDescription());
        existing.setFeatures(updatedPlan.getFeatures());
        
        SubscriptionPlan saved = subscriptionPlanRepository.save(existing);
        
        // Invalidate cache
        subscriptionPlanCache.invalidatePlan(saved.getId()).subscribe();
        subscriptionPlanCache.invalidateAccountPlans(saved.getSubscriptionAccountId()).subscribe();
        
        return saved;
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        plan.setIsActive(false);
        plan.setDeletedAt(java.time.LocalDateTime.now());
        subscriptionPlanRepository.save(plan);
        
        // Invalidate cache
        subscriptionPlanCache.invalidatePlan(planId).subscribe();
        subscriptionPlanCache.invalidateAccountPlans(plan.getSubscriptionAccountId()).subscribe();
    }
}

