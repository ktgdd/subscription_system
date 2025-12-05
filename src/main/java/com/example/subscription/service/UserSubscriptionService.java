package com.example.subscription.service;

import com.example.subscription.model.BookKeeping;
import com.example.subscription.model.UserSubscription;
import com.example.subscription.repository.UserSubscriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionService {
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            } else if ("EXTENDED".equals(eventType)) {
                extendUserSubscription(bookKeeping, afterState);
            }
            
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
        
        userSubscriptionRepository.save(subscription);
    }

    private void extendUserSubscription(BookKeeping bookKeeping, Map<String, Object> afterState) {
        UserSubscription existing = userSubscriptionRepository.findActiveSubscription(
                bookKeeping.getUserId(),
                bookKeeping.getSubscriptionAccountId(),
                bookKeeping.getDurationTypeId()
        ).orElseThrow(() -> new RuntimeException("Active subscription not found for extension"));

        existing.setEndDate(LocalDate.parse(afterState.get("end_date").toString()));
        existing.setLastUpdatedAt(LocalDateTime.now());
        
        userSubscriptionRepository.save(existing);
    }

    public java.util.List<UserSubscription> getUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserId(userId);
    }

    public java.util.List<UserSubscription> getActiveUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }
}
