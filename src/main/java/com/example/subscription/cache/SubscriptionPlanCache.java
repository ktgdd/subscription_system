package com.example.subscription.cache;

import com.example.subscription.config.AppProperties;
import com.example.subscription.model.SubscriptionPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanCache {

    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_PREFIX = "subscription:plan";
    private static final String ACCOUNT_PLANS_KEY = "account:plans";

    public Mono<Void> cachePlan(SubscriptionPlan plan) {
        String key = redisCacheService.buildKey(CACHE_PREFIX, plan.getId().toString());
        Duration ttl = Duration.ofSeconds(appProperties.getCache().getSubscriptionPlans().getTtl());
        
        try {
            String json = objectMapper.writeValueAsString(plan);
            return redisCacheService.set(key, json, ttl)
                    .then(redisCacheService.delete(redisCacheService.buildKey(ACCOUNT_PLANS_KEY, plan.getSubscriptionAccountId().toString())))
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Error caching subscription plan", e);
            return Mono.empty();
        }
    }

    public Mono<SubscriptionPlan> getPlan(Long planId) {
        String key = redisCacheService.buildKey(CACHE_PREFIX, planId.toString());
        return redisCacheService.get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, SubscriptionPlan.class));
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing subscription plan", e);
                        return Mono.empty();
                    }
                });
    }

    public Mono<Void> invalidatePlan(Long planId) {
        String key = redisCacheService.buildKey(CACHE_PREFIX, planId.toString());
        return redisCacheService.delete(key).then();
    }

    public Mono<Void> cacheAccountPlans(Long accountId, List<SubscriptionPlan> plans) {
        String key = redisCacheService.buildKey(ACCOUNT_PLANS_KEY, accountId.toString());
        Duration ttl = Duration.ofSeconds(appProperties.getCache().getSubscriptionPlans().getTtl());
        
        try {
            String json = objectMapper.writeValueAsString(plans);
            return redisCacheService.set(key, json, ttl).then();
        } catch (JsonProcessingException e) {
            log.error("Error caching account plans", e);
            return Mono.empty();
        }
    }

    public Mono<List<SubscriptionPlan>> getAccountPlans(Long accountId) {
        String key = redisCacheService.buildKey(ACCOUNT_PLANS_KEY, accountId.toString());
        return redisCacheService.get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, SubscriptionPlan.class)));
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing account plans", e);
                        return Mono.empty();
                    }
                });
    }

    public Mono<Void> invalidateAccountPlans(Long accountId) {
        String key = redisCacheService.buildKey(ACCOUNT_PLANS_KEY, accountId.toString());
        return redisCacheService.delete(key).then();
    }
}

