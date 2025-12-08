package com.example.subscription.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyKeyGeneratorTest {

    @Test
    void testGenerate() {
        // Given
        Long userId = 100L;
        Long accountId = 1L;
        Long durationTypeId = 2L;
        String requestId = "req-123";

        // When
        String result = IdempotencyKeyGenerator.generate(userId, accountId, durationTypeId, requestId);

        // Then
        assertEquals("100:1:2:req-123", result);
    }

    @Test
    void testGenerate_WithNullRequestId() {
        // Given
        Long userId = 100L;
        Long accountId = 1L;
        Long durationTypeId = 2L;
        String requestId = null;

        // When
        String result = IdempotencyKeyGenerator.generate(userId, accountId, durationTypeId, requestId);

        // Then
        assertEquals("100:1:2:null", result);
    }
}

