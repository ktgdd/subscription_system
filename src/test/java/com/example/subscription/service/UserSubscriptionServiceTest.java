package com.example.subscription.service;

import com.example.subscription.model.BookKeeping;
import com.example.subscription.model.UserSubscription;
import com.example.subscription.observability.BusinessMetrics;
import com.example.subscription.repository.UserSubscriptionRepository;
import com.example.subscription.service.impl.UserSubscriptionServiceImpl;
import com.example.subscription.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private BusinessMetrics businessMetrics;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserSubscriptionServiceImpl userSubscriptionService;

    private BookKeeping testBookKeeping;
    private UserSubscription existingSubscription;

    @BeforeEach
    void setUp() {
        testBookKeeping = new BookKeeping();
        testBookKeeping.setId(1L);
        testBookKeeping.setUserId(100L);
        testBookKeeping.setSubscriptionAccountId(1L);
        testBookKeeping.setDurationTypeId(2L);
        testBookKeeping.setEventType("SUBSCRIBED");
        testBookKeeping.setAfterState("{\"start_date\":\"2024-01-01\",\"end_date\":\"2024-01-31\",\"status\":\"ACTIVE\"}");

        existingSubscription = new UserSubscription();
        existingSubscription.setId(1L);
        existingSubscription.setUserId(100L);
        existingSubscription.setSubscriptionAccountId(1L);
        existingSubscription.setDurationTypeId(2L);
        existingSubscription.setStartDate(LocalDate.now());
        existingSubscription.setEndDate(LocalDate.now().plusDays(30));
        existingSubscription.setStatus("ACTIVE");
    }

    @Test
    void testMaterializeFromBookKeeping_NewSubscription() {
        // Given
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenReturn(existingSubscription);
        doNothing().when(notificationService).notifySubscriptionCreated(any(UserSubscription.class));
        doNothing().when(businessMetrics).recordSubscriptionCreated(anyLong(), anyString());
        doNothing().when(businessMetrics).recordBookKeepingEvent(anyString(), anyString());

        // When
        assertDoesNotThrow(() -> {
            userSubscriptionService.materializeFromBookKeeping(testBookKeeping);
        });

        // Then
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
        verify(notificationService).notifySubscriptionCreated(any(UserSubscription.class));
        verify(businessMetrics).recordSubscriptionCreated(anyLong(), anyString());
    }

    @Test
    void testMaterializeFromBookKeeping_ExtendSubscription() {
        // Given
        testBookKeeping.setEventType("EXTENDED");
        testBookKeeping.setAfterState("{\"end_date\":\"2024-02-29\"}");

        when(userSubscriptionRepository.findActiveSubscription(100L, 1L, 2L))
                .thenReturn(Optional.of(existingSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenReturn(existingSubscription);
        doNothing().when(notificationService).notifySubscriptionExtended(any(UserSubscription.class));
        doNothing().when(businessMetrics).recordSubscriptionExtended(anyLong(), anyString());
        doNothing().when(businessMetrics).recordBookKeepingEvent(anyString(), anyString());

        // When
        assertDoesNotThrow(() -> {
            userSubscriptionService.materializeFromBookKeeping(testBookKeeping);
        });

        // Then
        verify(userSubscriptionRepository).findActiveSubscription(100L, 1L, 2L);
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
        verify(notificationService).notifySubscriptionExtended(any(UserSubscription.class));
    }

    @Test
    void testGetUserSubscriptions() {
        // Given
        when(userSubscriptionRepository.findByUserId(100L))
                .thenReturn(List.of(existingSubscription));

        // When
        List<UserSubscription> result = userSubscriptionService.getUserSubscriptions(100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userSubscriptionRepository).findByUserId(100L);
    }

    @Test
    void testGetActiveUserSubscriptions() {
        // Given
        when(userSubscriptionRepository.findByUserIdAndStatus(100L, "ACTIVE"))
                .thenReturn(List.of(existingSubscription));

        // When
        List<UserSubscription> result = userSubscriptionService.getActiveUserSubscriptions(100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userSubscriptionRepository).findByUserIdAndStatus(100L, "ACTIVE");
    }
}

