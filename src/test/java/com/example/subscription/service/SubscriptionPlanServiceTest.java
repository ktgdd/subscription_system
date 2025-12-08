package com.example.subscription.service;

import com.example.subscription.cache.SubscriptionPlanCache;
import com.example.subscription.model.SubscriptionPlan;
import com.example.subscription.repository.SubscriptionPlanRepository;
import com.example.subscription.service.impl.SubscriptionPlanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private SubscriptionPlanCache subscriptionPlanCache;

    @InjectMocks
    private SubscriptionPlanServiceImpl subscriptionPlanService;

    private SubscriptionPlan testPlan;

    @BeforeEach
    void setUp() {
        testPlan = new SubscriptionPlan();
        testPlan.setId(1L);
        testPlan.setSubscriptionAccountId(1L);
        testPlan.setDurationTypeId(2L);
        testPlan.setAmount(new BigDecimal("9.99"));
        testPlan.setCurrency("USD");
        testPlan.setIsActive(true);
        testPlan.setCreatedAt(LocalDateTime.now());
        testPlan.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetPlanById_CacheHit() {
        // Given
        when(subscriptionPlanCache.getPlan(1L))
                .thenReturn(Mono.just(testPlan));

        // When
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(subscriptionPlanCache).getPlan(1L);
        verify(subscriptionPlanRepository, never()).findById(anyLong());
    }

    @Test
    void testGetPlanById_CacheMiss() {
        // Given
        when(subscriptionPlanCache.getPlan(1L))
                .thenReturn(Mono.empty());
        when(subscriptionPlanRepository.findById(1L))
                .thenReturn(Optional.of(testPlan));
        when(subscriptionPlanCache.cachePlan(any(SubscriptionPlan.class)))
                .thenReturn(Mono.empty());

        // When
        Optional<SubscriptionPlan> result = subscriptionPlanService.getPlanById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(subscriptionPlanRepository).findById(1L);
        verify(subscriptionPlanCache).cachePlan(any(SubscriptionPlan.class));
    }

    @Test
    void testCreatePlan() {
        // Given
        when(subscriptionPlanRepository.findActivePlanByAccountAndDurationType(1L, 2L))
                .thenReturn(Optional.empty());
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class)))
                .thenReturn(testPlan);
        when(subscriptionPlanCache.cachePlan(any(SubscriptionPlan.class)))
                .thenReturn(Mono.empty());
        when(subscriptionPlanCache.invalidateAccountPlans(anyLong()))
                .thenReturn(Mono.empty());

        // When
        SubscriptionPlan result = subscriptionPlanService.createPlan(testPlan);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(subscriptionPlanRepository).save(any(SubscriptionPlan.class));
        verify(subscriptionPlanCache).cachePlan(any(SubscriptionPlan.class));
    }

    @Test
    void testCreatePlan_DeactivatesOldPlan() {
        // Given
        SubscriptionPlan oldPlan = new SubscriptionPlan();
        oldPlan.setId(2L);
        oldPlan.setIsActive(true);

        when(subscriptionPlanRepository.findActivePlanByAccountAndDurationType(1L, 2L))
                .thenReturn(Optional.of(oldPlan));
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class)))
                .thenReturn(testPlan);
        when(subscriptionPlanCache.invalidatePlan(anyLong()))
                .thenReturn(Mono.empty());
        when(subscriptionPlanCache.cachePlan(any(SubscriptionPlan.class)))
                .thenReturn(Mono.empty());
        when(subscriptionPlanCache.invalidateAccountPlans(anyLong()))
                .thenReturn(Mono.empty());

        // When
        subscriptionPlanService.createPlan(testPlan);

        // Then
        verify(subscriptionPlanRepository).save(oldPlan);
        assertFalse(oldPlan.getIsActive());
        assertNotNull(oldPlan.getDeletedAt());
    }
}

