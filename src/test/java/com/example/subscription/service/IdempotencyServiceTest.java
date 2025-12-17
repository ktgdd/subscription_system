package com.example.subscription.service;

import com.example.subscription.config.AppProperties;
import com.example.subscription.exception.ErrorCode;
import com.example.subscription.exception.SubscriptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;
    
    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        // cleanup redis keys before each test
        redisTemplate.keys("idempotency:*")
                .flatMap(key -> redisTemplate.delete(key))
                .blockLast();
    }

    @Test
    void testFirstRequestSucceeds() {
        String key = "test-key-1";
        
        StepVerifier.create(idempotencyService.checkAndSet(key))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testDuplicateRequestFails() {
        String key = "test-key-2";
        
        idempotencyService.checkAndSet(key).block();
        
        // second request should fail
        StepVerifier.create(idempotencyService.checkAndSet(key))
                .expectErrorMatches(e -> 
                    e instanceof SubscriptionException &&
                    ((SubscriptionException) e).getErrorCode() == ErrorCode.DUPLICATE_REQUEST
                )
                .verify();
    }

    @Test
    void testDifferentKeysWork() {
        String key1 = "key1";
        String key2 = "key2";
        
        idempotencyService.checkAndSet(key1).block();
        idempotencyService.checkAndSet(key2).block();
        
        // both should work
        assertTrue(idempotencyService.exists(key1).block());
        assertTrue(idempotencyService.exists(key2).block());
    }

    @Test
    void testConcurrentRequestsWithSameKey() throws InterruptedException {
        // main test - concurrent requests with same key should only allow one
        String key = "concurrent-key";
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    Boolean result = idempotencyService.checkAndSet(key).block();
                    if (result != null && result) {
                        successCount.incrementAndGet();
                    }
                } catch (SubscriptionException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(numThreads - 1, failCount.get());
    }

    @Test
    void testConcurrentRequestsWithDifferentKeys() throws InterruptedException {
        // different keys should all succeed
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String key = "key-" + i;
            executor.submit(() -> {
                try {
                    idempotencyService.checkAndSet(key).block();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // shouldn't happen
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numThreads, successCount.get());
    }

    @Test
    void testKeyExpiresAfterTTL() throws InterruptedException {
        String key = "ttl-key";
        
        idempotencyService.checkAndSet(key).block();
        assertTrue(idempotencyService.exists(key).block());
        
        // wait for ttl to expire
        int ttl = appProperties.getIdempotency().getRedisTtlSeconds();
        Thread.sleep((ttl + 1) * 1000);
        
        assertFalse(idempotencyService.exists(key).block());
        
        // should be able to set again after expiry
        idempotencyService.checkAndSet(key).block();
    }
}

