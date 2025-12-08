package com.example.subscription.service.impl;

import com.example.subscription.model.BookKeeping;
import com.example.subscription.model.UserSubscription;
import com.example.subscription.notification.NotificationService;
import com.example.subscription.observability.BusinessMetrics;
import com.example.subscription.repository.UserSubscriptionRepository;
import com.example.subscription.service.UserSubscriptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionServiceImpl implements UserSubscriptionService {
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final BusinessMetrics businessMetrics;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void materializeFromBookKeeping(BookKeeping bookKeeping) {
        try {
            Map<String, Object> afterState = objectMapper.readValue(
                    bookKeeping.getAfterState(), 
                    new TypeReference<Map<String, Object>>() {}
            );

            String eventType = bookKeeping.getEventType();
            
            if ("SUBSCRIBED".equals(eventType)) {
                createUserSubscription(bookKeeping, afterState);
                businessMetrics.recordSubscriptionCreated(
                        bookKeeping.getSubscriptionAccountId(), 
                        bookKeeping.getDurationTypeId().toString());
            } else if ("EXTENDED".equals(eventType)) {
                extendUserSubscription(bookKeeping, afterState);
                businessMetrics.recordSubscriptionExtended(
                        bookKeeping.getSubscriptionAccountId(), 
                        bookKeeping.getDurationTypeId().toString());
            }
            
            businessMetrics.recordBookKeepingEvent(eventType, "PROCESSED");
            log.info("Materialized book keeping to user subscription: idempotencyKey={}", 
                    bookKeeping.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Error materializing book keeping", e);
            throw new RuntimeException("Failed to materialize book keeping", e);
        }
    }

    private void createUserSubscription(BookKeeping bookKeeping, Map<String, Object> afterState) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(bookKeeping.getUserId());
        subscription.setSubscriptionAccountId(bookKeeping.getSubscriptionAccountId());
        subscription.setDurationTypeId(bookKeeping.getDurationTypeId());
        subscription.setStartDate(LocalDate.parse(afterState.get("start_date").toString()));
        subscription.setEndDate(LocalDate.parse(afterState.get("end_date").toString()));
        subscription.setStatus(afterState.get("status").toString());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setLastUpdatedAt(LocalDateTime.now());
        
        UserSubscription saved = userSubscriptionRepository.save(subscription);
        
        // Send notification
        notificationService.notifySubscriptionCreated(saved);
    }

    private void extendUserSubscription(BookKeeping bookKeeping, Map<String, Object> afterState) {
        UserSubscription existing = userSubscriptionRepository.findActiveSubscription(
                bookKeeping.getUserId(),
                bookKeeping.getSubscriptionAccountId(),
                bookKeeping.getDurationTypeId()
        ).orElseThrow(() -> new RuntimeException("Active subscription not found for extension"));

        existing.setEndDate(LocalDate.parse(afterState.get("end_date").toString()));
        existing.setLastUpdatedAt(LocalDateTime.now());
        
        UserSubscription saved = userSubscriptionRepository.save(existing);
        
        // Send notification
        notificationService.notifySubscriptionExtended(saved);
    }

    @Override
    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserId(userId);
    }

    @Override
    public List<UserSubscription> getActiveUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }
}

