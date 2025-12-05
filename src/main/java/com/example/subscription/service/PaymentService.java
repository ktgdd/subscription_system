package com.example.subscription.service;

import com.example.subscription.model.BookKeeping;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface PaymentService {
    
    void processPayment(BookKeeping bookKeeping);
    
    Mono<Map<String, Object>> getPaymentStatus(String paymentReferenceId);
}
