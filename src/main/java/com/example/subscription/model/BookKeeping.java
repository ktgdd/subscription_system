package com.example.subscription.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_keeping")
@Data
public class BookKeeping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(name = "subscription_plan_id", nullable = false)
    private Long subscriptionPlanId;
    
    @Column(name = "subscription_account_id", nullable = false)
    private Long subscriptionAccountId;
    
    @Column(name = "duration_type_id", nullable = false)
    private Long durationTypeId;
    
    @Column(nullable = false)
    private String eventType; // SUBSCRIBED, EXTENDED, CANCELLED, EXPIRED
    
    @Column(nullable = false)
    private String status; // INITIATED, COMPLETED, PROCESSED, FAILED
    
    @JdbcTypeCode(SqlTypes.JSON)
    private String beforeState;
    
    @JdbcTypeCode(SqlTypes.JSON)
    private String afterState;
    
    private String paymentReferenceId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;
    
    private LocalDateTime processedAt;
    
    @Column(nullable = false)
    private Integer retryCount = 0;
    
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

