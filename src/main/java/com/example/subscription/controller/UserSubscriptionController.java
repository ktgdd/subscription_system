package com.example.subscription.controller;

import com.example.subscription.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class UserSubscriptionController {

    // TODO: Implement user subscription operations
    // POST /api/subscriptions - Subscribe to a plan
    // POST /api/subscriptions/{id}/extend - Extend subscription
    // GET /api/subscriptions - Get user's subscriptions
    // GET /api/subscriptions/{id} - Get subscription by id

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUserSubscriptions() {
        // TODO: Implement
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet"));
    }
}

