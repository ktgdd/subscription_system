package com.example.subscription.controller;

import com.example.subscription.dto.request.ExtendSubscriptionRequest;
import com.example.subscription.dto.request.SubscribeRequest;
import com.example.subscription.dto.response.ApiResponse;
import com.example.subscription.dto.response.UserSubscriptionResponse;
import com.example.subscription.exception.ErrorCode;
import com.example.subscription.exception.SubscriptionException;
import com.example.subscription.model.*;
import com.example.subscription.repository.*;
import com.example.subscription.service.*;
import com.example.subscription.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final BookKeepingService bookKeepingService;
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionAccountRepository accountRepository;
    private final DurationTypeRepository durationTypeRepository;
    private final RulesEngineRepository rulesEngineRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        String requestId = (String) httpRequest.getAttribute("requestId");

        // Get subscription plan
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Plan not found"));

        // Check idempotency
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                userId, plan.getSubscriptionAccountId(), plan.getDurationTypeId(), requestId);
        
        idempotencyService.checkAndSet(idempotencyKey).block();

        // Check if already exists in book keeping
        BookKeeping existing = bookKeepingService.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            throw new SubscriptionException(ErrorCode.DUPLICATE_REQUEST);
        }

        // Calculate dates
        LocalDate startDate = LocalDate.now();
        DurationType durationType = durationTypeRepository.findById(plan.getDurationTypeId())
                .orElseThrow(() -> new SubscriptionException(ErrorCode.INTERNAL_ERROR, "Duration type not found"));
        LocalDate endDate = DateCalculator.calculateEndDate(startDate, durationType.getDays());

        // Create book keeping entry
        BookKeeping bookKeeping = new BookKeeping();
        bookKeeping.setIdempotencyKey(idempotencyKey);
        bookKeeping.setUserId(userId);
        bookKeeping.setSubscriptionPlanId(plan.getId());
        bookKeeping.setSubscriptionAccountId(plan.getSubscriptionAccountId());
        bookKeeping.setDurationTypeId(plan.getDurationTypeId());
        bookKeeping.setEventType("SUBSCRIBED");

        Map<String, Object> afterState = new HashMap<>();
        afterState.put("start_date", startDate.toString());
        afterState.put("end_date", endDate.toString());
        afterState.put("status", "ACTIVE");

        try {
            bookKeeping.setAfterState(objectMapper.writeValueAsString(afterState));
        } catch (Exception e) {
            throw new SubscriptionException(ErrorCode.INTERNAL_ERROR, "Failed to serialize state", e);
        }

        BookKeeping saved = bookKeepingService.createBookKeepingEntry(bookKeeping);

        // Process payment asynchronously
        paymentService.processPayment(saved);

        Map<String, Object> response = Map.of(
            "bookKeepingId", saved.getId(),
            "message", "Subscription initiated. Payment processing in progress."
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Subscription initiated", response));
    }

    @PostMapping("/{id}/extend")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> extendSubscription(
            @PathVariable Long id,
            @Valid @RequestBody ExtendSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        String requestId = (String) httpRequest.getAttribute("requestId");

        // Get existing subscription
        UserSubscription existing = userSubscriptionService.getUserSubscriptions(userId).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Subscription not found"));

        // Get active plan for this account and duration type
        SubscriptionPlan plan = subscriptionPlanRepository
                .findActivePlanByAccountAndDurationType(
                        existing.getSubscriptionAccountId(), 
                        existing.getDurationTypeId())
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Active plan not found"));

        // Check idempotency
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                userId, existing.getSubscriptionAccountId(), existing.getDurationTypeId(), requestId);
        
        idempotencyService.checkAndSet(idempotencyKey).block();

        // Check if already exists in book keeping
        BookKeeping existingBookKeeping = bookKeepingService.findByIdempotencyKey(idempotencyKey);
        if (existingBookKeeping != null) {
            throw new SubscriptionException(ErrorCode.DUPLICATE_REQUEST);
        }

        // Get duration type
        DurationType durationType = durationTypeRepository.findById(existing.getDurationTypeId())
                .orElseThrow(() -> new SubscriptionException(ErrorCode.INTERNAL_ERROR, "Duration type not found"));

        // Calculate new end date (extend from current end date)
        LocalDate newEndDate = DateCalculator.extendEndDate(existing.getEndDate(), durationType.getDays());

        // Create book keeping entry
        BookKeeping bookKeeping = new BookKeeping();
        bookKeeping.setIdempotencyKey(idempotencyKey);
        bookKeeping.setUserId(userId);
        bookKeeping.setSubscriptionPlanId(plan.getId());
        bookKeeping.setSubscriptionAccountId(existing.getSubscriptionAccountId());
        bookKeeping.setDurationTypeId(existing.getDurationTypeId());
        bookKeeping.setEventType("EXTENDED");

        Map<String, Object> beforeState = new HashMap<>();
        beforeState.put("end_date", existing.getEndDate().toString());
        beforeState.put("status", existing.getStatus());

        Map<String, Object> afterState = new HashMap<>();
        afterState.put("end_date", newEndDate.toString());
        afterState.put("status", existing.getStatus());

        try {
            bookKeeping.setBeforeState(objectMapper.writeValueAsString(beforeState));
            bookKeeping.setAfterState(objectMapper.writeValueAsString(afterState));
        } catch (Exception e) {
            throw new SubscriptionException(ErrorCode.INTERNAL_ERROR, "Failed to serialize state", e);
        }

        BookKeeping saved = bookKeepingService.createBookKeepingEntry(bookKeeping);

        // Process payment asynchronously
        paymentService.processPayment(saved);

        Map<String, Object> response = Map.of(
            "bookKeepingId", saved.getId(),
            "newEndDate", newEndDate.toString(),
            "message", "Subscription extension initiated. Payment processing in progress."
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Extension initiated", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSubscriptionResponse>>> getUserSubscriptions(
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<UserSubscription> subscriptions = userSubscriptionService.getUserSubscriptions(userId);

        List<UserSubscriptionResponse> responses = subscriptions.stream()
                .map(subscription -> {
                    SubscriptionAccount account = accountRepository.findById(subscription.getSubscriptionAccountId()).orElse(null);
                    DurationType durationType = durationTypeRepository.findById(subscription.getDurationTypeId()).orElse(null);
                    return UserSubscriptionMapper.toResponse(subscription, account, durationType);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> getSubscriptionById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        UserSubscription subscription = userSubscriptionService.getUserSubscriptions(userId).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Subscription not found"));

        SubscriptionAccount account = accountRepository.findById(subscription.getSubscriptionAccountId()).orElse(null);
        DurationType durationType = durationTypeRepository.findById(subscription.getDurationTypeId()).orElse(null);
        
        UserSubscriptionResponse response = UserSubscriptionMapper.toResponse(subscription, account, durationType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
