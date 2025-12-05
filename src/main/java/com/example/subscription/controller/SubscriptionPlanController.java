package com.example.subscription.controller;

import com.example.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.example.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.example.subscription.dto.response.ApiResponse;
import com.example.subscription.dto.response.SubscriptionPlanResponse;
import com.example.subscription.exception.ErrorCode;
import com.example.subscription.exception.SubscriptionException;
import com.example.subscription.model.DurationType;
import com.example.subscription.model.SubscriptionAccount;
import com.example.subscription.model.SubscriptionPlan;
import com.example.subscription.repository.DurationTypeRepository;
import com.example.subscription.repository.SubscriptionAccountRepository;
import com.example.subscription.service.SubscriptionPlanService;
import com.example.subscription.util.SubscriptionPlanMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionAccountRepository accountRepository;
    private final DurationTypeRepository durationTypeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAllPlans(
            @RequestParam(required = false) Long accountId) {
        
        List<SubscriptionPlan> plans;
        if (accountId != null) {
            plans = subscriptionPlanService.getActivePlansByAccount(accountId);
        } else {
            // For simplicity, return empty list if no accountId specified
            // In production, you might want to return all plans or paginate
            plans = List.of();
        }

        List<SubscriptionPlanResponse> responses = plans.stream()
                .map(plan -> {
                    SubscriptionAccount account = accountRepository.findById(plan.getSubscriptionAccountId()).orElse(null);
                    DurationType durationType = durationTypeRepository.findById(plan.getDurationTypeId()).orElse(null);
                    return SubscriptionPlanMapper.toResponse(plan, account, durationType);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanById(@PathVariable Long id) {
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(id)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Plan not found: " + id));

        SubscriptionAccount account = accountRepository.findById(plan.getSubscriptionAccountId()).orElse(null);
        DurationType durationType = durationTypeRepository.findById(plan.getDurationTypeId()).orElse(null);
        
        SubscriptionPlanResponse response = SubscriptionPlanMapper.toResponse(plan, account, durationType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        
        SubscriptionPlan plan = SubscriptionPlanMapper.toEntity(request);
        SubscriptionPlan created = subscriptionPlanService.createPlan(plan);

        SubscriptionAccount account = accountRepository.findById(created.getSubscriptionAccountId()).orElse(null);
        DurationType durationType = durationTypeRepository.findById(created.getDurationTypeId()).orElse(null);
        
        SubscriptionPlanResponse response = SubscriptionPlanMapper.toResponse(created, account, durationType);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Plan created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
        
        SubscriptionPlan existing = subscriptionPlanService.getPlanById(id)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "Plan not found: " + id));

        if (request.getAmount() != null) existing.setAmount(request.getAmount());
        if (request.getCurrency() != null) existing.setCurrency(request.getCurrency());
        if (request.getName() != null) existing.setName(request.getName());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getFeatures() != null) existing.setFeatures(request.getFeatures());

        SubscriptionPlan updated = subscriptionPlanService.updatePlan(id, existing);

        SubscriptionAccount account = accountRepository.findById(updated.getSubscriptionAccountId()).orElse(null);
        DurationType durationType = durationTypeRepository.findById(updated.getDurationTypeId()).orElse(null);
        
        SubscriptionPlanResponse response = SubscriptionPlanMapper.toResponse(updated, account, durationType);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Plan deleted successfully", null));
    }
}
