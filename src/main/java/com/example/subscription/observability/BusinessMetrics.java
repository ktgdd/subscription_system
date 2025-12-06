package com.example.subscription.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // Subscription metrics
    public void recordSubscriptionCreated(Long accountId, String durationType) {
        Counter.builder("subscription.created")
                .tag("account_id", String.valueOf(accountId))
                .tag("duration_type", durationType)
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionExtended(Long accountId, String durationType) {
        Counter.builder("subscription.extended")
                .tag("account_id", String.valueOf(accountId))
                .tag("duration_type", durationType)
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionCancelled(Long accountId) {
        Counter.builder("subscription.cancelled")
                .tag("account_id", String.valueOf(accountId))
                .register(meterRegistry)
                .increment();
    }

    public void recordActiveSubscriptions(Long count) {
        meterRegistry.gauge("subscription.active.count", count);
    }

    // Plan metrics
    public void recordPlanCreated(Long accountId) {
        Counter.builder("subscription.plan.created")
                .tag("account_id", String.valueOf(accountId))
                .register(meterRegistry)
                .increment();
    }

    public void recordPlanUpdated(Long accountId) {
        Counter.builder("subscription.plan.updated")
                .tag("account_id", String.valueOf(accountId))
                .register(meterRegistry)
                .increment();
    }

    // Cache metrics
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hit")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.miss")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
                .increment();
    }

    // Payment metrics
    public void recordPaymentProcessed(String status) {
        Counter.builder("payment.processed")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentDuration(Timer.Sample sample, String status) {
        if (sample != null) {
            sample.stop(Timer.builder("payment.duration")
                    .tag("status", status)
                    .register(meterRegistry));
        }
    }

    // Book keeping metrics
    public void recordBookKeepingEvent(String eventType, String status) {
        Counter.builder("bookkeeping.event")
                .tag("event_type", eventType)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }
}

