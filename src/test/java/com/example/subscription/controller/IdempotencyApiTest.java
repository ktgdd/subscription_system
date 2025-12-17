package com.example.subscription.controller;

import com.example.subscription.dto.request.SubscribeRequest;
import com.example.subscription.model.*;
import com.example.subscription.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdempotencyApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionAccountRepository subscriptionAccountRepository;

    @Autowired
    private DurationTypeRepository durationTypeRepository;

    @Autowired
    private BookKeepingRepository bookKeepingRepository;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    private MockMvc mockMvc;
    private SubscriptionAccount testAccount;
    private DurationType testDurationType;
    private SubscriptionPlan testPlan;
    private Long testUserId = 123L;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();

        // cleanup old data
        bookKeepingRepository.deleteAll();
        subscriptionPlanRepository.deleteAll();
        subscriptionAccountRepository.deleteAll();
        durationTypeRepository.deleteAll();
        
        // clear redis
        redisTemplate.keys("idempotency:*")
                .flatMap(key -> redisTemplate.delete(key))
                .blockLast();

        // create test account
        testAccount = new SubscriptionAccount();
        testAccount.setName("Test Netflix");
        testAccount.setIsActive(true);
        testAccount = subscriptionAccountRepository.save(testAccount);

        // create duration type
        testDurationType = new DurationType();
        testDurationType.setType("MONTHLY");
        testDurationType.setDays(30);
        testDurationType = durationTypeRepository.save(testDurationType);

        // create plan
        testPlan = new SubscriptionPlan();
        testPlan.setSubscriptionAccountId(testAccount.getId());
        testPlan.setDurationTypeId(testDurationType.getId());
        testPlan.setAmount(new BigDecimal("9.99"));
        testPlan.setCurrency("USD");
        testPlan.setName("Basic Plan");
        testPlan.setIsActive(true);
        testPlan = subscriptionPlanRepository.save(testPlan);
    }

    @Test
    void testSubscribeWithSameRequestId_SecondRequestFails() throws Exception {
        String requestId = UUID.randomUUID().toString();
        SubscribeRequest request = new SubscribeRequest();
        request.setSubscriptionPlanId(testPlan.getId());

        // first request should work
        mockMvc.perform(post("/api/subscriptions")
                        .header("X-User-Id", testUserId.toString())
                        .header("X-Request-Id", requestId)
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // same request id should be rejected
        mockMvc.perform(post("/api/subscriptions")
                        .header("X-User-Id", testUserId.toString())
                        .header("X-Request-Id", requestId)
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 400 || status == 409, "Expected 400 or 409, got " + status);
                });
    }

    @Test
    void testConcurrentRequestsSameRequestId() throws Exception {
        // test that concurrent requests with same request id only allow one
        String requestId = UUID.randomUUID().toString();
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        SubscribeRequest request = new SubscribeRequest();
        request.setSubscriptionPlanId(testPlan.getId());

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/subscriptions")
                                    .header("X-User-Id", testUserId.toString())
                                    .header("X-Request-Id", requestId)
                                    .header("X-Role", "USER")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 202) {
                                    successCount.incrementAndGet();
                                } else if (status == 400 || status == 409) {
                                    failCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    // swallow exceptions for now
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // only one should succeed
        assertEquals(1, successCount.get());
        assertEquals(numThreads - 1, failCount.get());
    }

    @Test
    void testConcurrentRequestsDifferentRequestIds() throws Exception {
        // different request ids should all work
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        SubscribeRequest request = new SubscribeRequest();
        request.setSubscriptionPlanId(testPlan.getId());

        for (int i = 0; i < numThreads; i++) {
            final String requestId = UUID.randomUUID().toString();
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/subscriptions")
                                    .header("X-User-Id", testUserId.toString())
                                    .header("X-Request-Id", requestId)
                                    .header("X-Role", "USER")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(result -> {
                                if (result.getResponse().getStatus() == 202) {
                                    successCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    // ignore errors
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numThreads, successCount.get());
    }
}

