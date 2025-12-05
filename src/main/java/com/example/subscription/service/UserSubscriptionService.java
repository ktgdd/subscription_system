package com.example.subscription.service;

import com.example.subscription.model.BookKeeping;
import com.example.subscription.model.UserSubscription;

import java.util.List;

public interface UserSubscriptionService {
    
    void materializeFromBookKeeping(BookKeeping bookKeeping);
    
    List<UserSubscription> getUserSubscriptions(Long userId);
    
    List<UserSubscription> getActiveUserSubscriptions(Long userId);
}
