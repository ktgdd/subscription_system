package com.example.subscription.config;

import com.example.subscription.cache.SubscriptionPlanCache;
import com.example.subscription.model.SubscriptionPlan;
import com.example.subscription.repository.SubscriptionAccountRepository;
import com.example.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupConfig implements CommandLineRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionAccountRepository subscriptionAccountRepository;
    private final SubscriptionPlanCache subscriptionPlanCache;

    @Override
    public void run(String... args) {
        log.info("Starting cache warm-up...");
        
        try {
            // Load all active subscription plans
            List<SubscriptionPlan> activePlans = subscriptionPlanRepository.findAll().stream()
                    .filter(plan -> plan.getIsActive() != null && plan.getIsActive() 
                            && plan.getDeletedAt() == null)
                    .toList();

            // Cache each plan individually
            activePlans.forEach(plan -> {
                subscriptionPlanCache.cachePlan(plan).subscribe();
                log.debug("Cached subscription plan: id={}, accountId={}", 
                        plan.getId(), plan.getSubscriptionAccountId());
            });

            // Cache plans grouped by account
            subscriptionAccountRepository.findAll().stream()
                    .filter(account -> account.getIsActive() != null && account.getIsActive())
                    .forEach(account -> {
                        List<SubscriptionPlan> accountPlans = subscriptionPlanRepository
                                .findBySubscriptionAccountIdAndIsActiveTrueAndDeletedAtIsNull(account.getId());
                        if (!accountPlans.isEmpty()) {
                            subscriptionPlanCache.cacheAccountPlans(account.getId(), accountPlans).subscribe();
                            log.debug("Cached plans for account: accountId={}, planCount={}", 
                                    account.getId(), accountPlans.size());
                        }
                    });

            log.info("Cache warm-up completed: {} plans cached", activePlans.size());
        } catch (Exception e) {
            log.error("Error during cache warm-up", e);
            // Don't fail startup if cache warm-up fails
        }
    }
}

