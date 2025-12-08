package com.example.subscription.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreaker getPaymentServiceCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("paymentService");
    }

    public CircuitBreaker.State getPaymentServiceState() {
        return getPaymentServiceCircuitBreaker().getState();
    }
}

