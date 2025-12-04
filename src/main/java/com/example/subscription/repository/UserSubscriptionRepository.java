package com.example.subscription.repository;

import com.example.subscription.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    List<UserSubscription> findByUserId(Long userId);
    
    List<UserSubscription> findByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.userId = :userId " +
           "AND us.subscriptionAccountId = :accountId AND us.durationTypeId = :durationTypeId " +
           "AND us.status = 'ACTIVE'")
    Optional<UserSubscription> findActiveSubscription(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("durationTypeId") Long durationTypeId);
}

