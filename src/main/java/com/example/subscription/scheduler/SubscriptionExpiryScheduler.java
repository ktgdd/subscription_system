package com.example.subscription.scheduler;

import com.example.subscription.model.UserSubscription;
import com.example.subscription.notification.NotificationService;
import com.example.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final NotificationService notificationService;

    // Run daily at 9 AM to check for expiring subscriptions
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpiringSubscriptions() {
        log.info("Running scheduled task: Checking expiring subscriptions");
        
        try {
            List<UserSubscription> activeSubscriptions = userSubscriptionRepository
                    .findByStatus("ACTIVE");
            
            // Filter to only active ones with valid end dates
            List<UserSubscription> validActiveSubscriptions = activeSubscriptions.stream()
                    .filter(sub -> sub.getStatus().equals("ACTIVE") && sub.getEndDate() != null)
                    .toList();
            
            notificationService.checkAndNotifyExpiringSubscriptions(validActiveSubscriptions);
            
            log.info("Completed scheduled task: Checked {} active subscriptions", validActiveSubscriptions.size());
        } catch (Exception e) {
            log.error("Error in scheduled task for expiring subscriptions", e);
        }
    }
}

