package com.example.subscription.controller;

import com.example.subscription.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    // TODO: Implement CRUD operations for subscription plans
    // GET /api/subscription-plans - List all plans
    // GET /api/subscription-plans/{id} - Get plan by id
    // POST /api/subscription-plans - Create plan (Admin only)
    // PUT /api/subscription-plans/{id} - Update plan (Admin only)
    // DELETE /api/subscription-plans/{id} - Soft delete plan (Admin only)

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllPlans() {
        // TODO: Implement
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet"));
    }
}

