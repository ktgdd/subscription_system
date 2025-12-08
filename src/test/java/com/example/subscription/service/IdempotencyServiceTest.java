package com.example.subscription.service;

import com.example.subscription.cache.RedisCacheService;
import com.example.subscription.config.AppProperties;
import com.example.subscription.exception.SubscriptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        AppProperties.Idempotency idempotency = new AppProperties.Idempotency();
        idempotency.setRedisTtlSeconds(10);
        when(appProperties.getIdempotency()).thenReturn(idempotency);
    }

    @Test
    void testCheckAndSet_Success() {
        // Given
        String idempotencyKey = "test:1:2:req123";
        when(redisCacheService.setIfAbsent(anyString(), anyString(), any()))
                .thenReturn(Mono.just(true));

        // When
        assertDoesNotThrow(() -> {
            idempotencyService.checkAndSet(idempotencyKey).block();
        });

        // Then
        verify(redisCacheService).setIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void testCheckAndSet_Duplicate() {
        // Given
        String idempotencyKey = "test:1:2:req123";
        when(redisCacheService.setIfAbsent(anyString(), anyString(), any()))
                .thenReturn(Mono.just(false));

        // When & Then
        assertThrows(SubscriptionException.class, () -> {
            idempotencyService.checkAndSet(idempotencyKey).block();
        });
    }
}

