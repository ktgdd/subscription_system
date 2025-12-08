package com.example.subscription.service.impl;

import com.example.subscription.config.AppProperties;
import com.example.subscription.model.BookKeeping;
import com.example.subscription.observability.BusinessMetrics;
import com.example.subscription.resilience.CircuitBreakerService;
import com.example.subscription.service.BookKeepingService;
import com.example.subscription.service.PaymentService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final BookKeepingService bookKeepingService;
    private final BusinessMetrics businessMetrics;
    private final CircuitBreakerService circuitBreakerService;

    @Override
    public void processPayment(BookKeeping bookKeeping) {
        Timer.Sample timer = businessMetrics.startPaymentTimer();
        CircuitBreaker circuitBreaker = circuitBreakerService.getPaymentServiceCircuitBreaker();
        
        // Check circuit breaker state
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker is OPEN for payment service, skipping payment processing: bookKeepingId={}", 
                    bookKeeping.getId());
            businessMetrics.recordPaymentProcessed("circuit_breaker_open");
            return;
        }

        WebClient webClient = webClientBuilder
                .baseUrl(appProperties.getPayment().getServiceUrl())
                .build();

        Map<String, Object> paymentRequest = Map.of(
            "bookKeepingId", bookKeeping.getId(),
            "userId", bookKeeping.getUserId(),
            "amount", "0.00", // Amount would come from subscription plan
            "subscriptionPlanId", bookKeeping.getSubscriptionPlanId()
        );

        // Wrap in circuit breaker using Resilience4j Reactor
        webClient.post()
                .uri("/process")
                .bodyValue(paymentRequest)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Payment service error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Payment processing failed"));
                })
                .bodyToMono(Map.class)
                .transform(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(Retry.fixedDelay(
                    appProperties.getPayment().getRetry().getMaxAttempts(),
                    Duration.ofMillis(appProperties.getPayment().getRetry().getDelay())
                ))
                .subscribe(
                    result -> {
                        String paymentReferenceId = (String) result.get("paymentReferenceId");
                        bookKeepingService.markAsCompleted(bookKeeping.getId(), paymentReferenceId);
                        businessMetrics.recordPaymentProcessed("success");
                        businessMetrics.recordPaymentDuration(timer, "success");
                        log.info("Payment processed successfully: bookKeepingId={}, paymentReferenceId={}", 
                                bookKeeping.getId(), paymentReferenceId);
                    },
                    error -> {
                        businessMetrics.recordPaymentProcessed("failed");
                        businessMetrics.recordPaymentDuration(timer, "failed");
                        log.error("Payment processing failed after retries: bookKeepingId={}", 
                                bookKeeping.getId(), error);
                    }
                );
    }

    @Override
    public Mono<Map<String, Object>> getPaymentStatus(String paymentReferenceId) {
        WebClient webClient = webClientBuilder
                .baseUrl(appProperties.getPayment().getServiceUrl())
                .build();

        return webClient.get()
                .uri("/status/{paymentReferenceId}", paymentReferenceId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .timeout(Duration.ofMillis(appProperties.getPayment().getTimeout()))
                .retryWhen(Retry.fixedDelay(
                    appProperties.getPayment().getRetry().getMaxAttempts(),
                    Duration.ofMillis(appProperties.getPayment().getRetry().getDelay())
                ));
    }
}

