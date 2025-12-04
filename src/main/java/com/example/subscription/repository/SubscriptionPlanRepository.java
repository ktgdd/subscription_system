package com.example.subscription.repository;

import com.example.subscription.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    
    List<SubscriptionPlan> findBySubscriptionAccountIdAndIsActiveTrueAndDeletedAtIsNull(Long accountId);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.subscriptionAccountId = :accountId " +
           "AND sp.durationTypeId = :durationTypeId AND sp.isActive = true AND sp.deletedAt IS NULL")
    Optional<SubscriptionPlan> findActivePlanByAccountAndDurationType(
            @Param("accountId") Long accountId, 
            @Param("durationTypeId") Long durationTypeId);
}

