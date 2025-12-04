package com.example.subscription.util;

public class IdempotencyKeyGenerator {
    
    public static String generate(Long userId, Long accountId, Long durationTypeId, String requestId) {
        return String.format("%d:%d:%d:%s", userId, accountId, durationTypeId, requestId);
    }
}

