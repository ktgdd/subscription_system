package com.example.subscription.notification;

import com.example.subscription.model.UserSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // Extensible notification service foundation
    // Can be extended to send emails, SMS, push notifications, etc.

    public void notifySubscriptionCreated(UserSubscription subscription) {
        log.info("Notification: Subscription created - userId={}, subscriptionId={}, accountId={}", 
                subscription.getUserId(), subscription.getId(), subscription.getSubscriptionAccountId());
        // TODO: Send notification (email, SMS, etc.)
    }

    public void notifySubscriptionExtended(UserSubscription subscription) {
        log.info("Notification: Subscription extended - userId={}, subscriptionId={}, newEndDate={}", 
                subscription.getUserId(), subscription.getId(), subscription.getEndDate());
        // TODO: Send notification
    }

    public void notifySubscriptionExpiring(UserSubscription subscription, long daysUntilExpiry) {
        log.info("Notification: Subscription expiring soon - userId={}, subscriptionId={}, daysUntilExpiry={}", 
                subscription.getUserId(), subscription.getId(), daysUntilExpiry);
        // TODO: Send notification
    }

    public void checkAndNotifyExpiringSubscriptions(List<UserSubscription> activeSubscriptions) {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(7); // Warn 7 days before expiry

        activeSubscriptions.stream()
                .filter(sub -> sub.getStatus().equals("ACTIVE") && sub.getEndDate() != null)
                .filter(sub -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, sub.getEndDate());
                    return daysUntilExpiry > 0 && daysUntilExpiry <= 7 && 
                           sub.getEndDate().isBefore(warningDate.plusDays(1));
                })
                .forEach(sub -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, sub.getEndDate());
                    notifySubscriptionExpiring(sub, daysUntilExpiry);
                });
    }
}

