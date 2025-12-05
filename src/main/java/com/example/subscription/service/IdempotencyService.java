package com.example.subscription.service;

import com.example.subscription.cache.RedisCacheService;
import com.example.subscription.config.AppProperties;
import com.example.subscription.exception.ErrorCode;
import com.example.subscription.exception.SubscriptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;
    
    private static final String IDEMPOTENCY_PREFIX = "idempotency";

    public Mono<Boolean> checkAndSet(String idempotencyKey) {
        String key = redisCacheService.buildKey(IDEMPOTENCY_PREFIX, idempotencyKey);
        Duration ttl = Duration.ofSeconds(appProperties.getIdempotency().getRedisTtlSeconds());
        
        return redisCacheService.setIfAbsent(key, "1", ttl)
                .flatMap(set -> {
                    if (Boolean.FALSE.equals(set)) {
                        log.warn("Duplicate request detected: {}", idempotencyKey);
                        return Mono.error(new SubscriptionException(
                            ErrorCode.DUPLICATE_REQUEST,
                            "Duplicate request detected. Please wait before retrying"
                        ));
                    }
                    return Mono.just(true);
                });
    }

    public Mono<Boolean> exists(String idempotencyKey) {
        String key = redisCacheService.buildKey(IDEMPOTENCY_PREFIX, idempotencyKey);
        return redisCacheService.exists(key);
    }
}

