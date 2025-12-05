package com.example.subscription.service;

import com.example.subscription.config.AppProperties;
import com.example.subscription.model.BookKeeping;
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
public class PaymentService {
    
    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final BookKeepingService bookKeepingService;

    public void processPayment(BookKeeping bookKeeping) {
        WebClient webClient = webClientBuilder
                .baseUrl(appProperties.getPayment().getServiceUrl())
                .build();

        Map<String, Object> paymentRequest = Map.of(
            "bookKeepingId", bookKeeping.getId(),
            "userId", bookKeeping.getUserId(),
            "amount", "0.00", // Amount would come from subscription plan
            "subscriptionPlanId", bookKeeping.getSubscriptionPlanId()
        );

        webClient.post()
                .uri("/process")
                .bodyValue(paymentRequest)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Payment service error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Payment processing failed"));
                })
                .bodyToMono(Map.class)
                .retryWhen(Retry.fixedDelay(
                    appProperties.getPayment().getRetry().getMaxAttempts(),
                    Duration.ofMillis(appProperties.getPayment().getRetry().getDelay())
                ))
                .subscribe(
                    result -> {
                        String paymentReferenceId = (String) result.get("paymentReferenceId");
                        bookKeepingService.markAsCompleted(bookKeeping.getId(), paymentReferenceId);
                        log.info("Payment processed successfully: bookKeepingId={}, paymentReferenceId={}", 
                                bookKeeping.getId(), paymentReferenceId);
                    },
                    error -> {
                        log.error("Payment processing failed after retries: bookKeepingId={}", 
                                bookKeeping.getId(), error);
                    }
                );
    }

    @SuppressWarnings("unchecked")
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
