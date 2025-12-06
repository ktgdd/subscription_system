package com.example.subscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Rules rules = new Rules();
    private Payment payment = new Payment();
    private Observability observability = new Observability();
    private ThreadPool threadPool = new ThreadPool();
    private Cache cache = new Cache();
    private Idempotency idempotency = new Idempotency();
    private Kafka kafka = new Kafka();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Rules getRules() {
        return rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Observability getObservability() {
        return observability;
    }

    public void setObservability(Observability observability) {
        this.observability = observability;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(Idempotency idempotency) {
        this.idempotency = idempotency;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public static class Jwt {
        private String secret;
        private long expiration;
        private String userIdClaim = "user_id";
        private String roleClaim = "role";
        private String requestIdClaim = "request_id";
        private boolean debugMode = false;
        private boolean debugSkipExpiryValidation = false;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public String getUserIdClaim() {
            return userIdClaim;
        }

        public void setUserIdClaim(String userIdClaim) {
            this.userIdClaim = userIdClaim;
        }

        public String getRoleClaim() {
            return roleClaim;
        }

        public void setRoleClaim(String roleClaim) {
            this.roleClaim = roleClaim;
        }

        public String getRequestIdClaim() {
            return requestIdClaim;
        }

        public void setRequestIdClaim(String requestIdClaim) {
            this.requestIdClaim = requestIdClaim;
        }

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }

        public boolean isDebugSkipExpiryValidation() {
            return debugSkipExpiryValidation;
        }

        public void setDebugSkipExpiryValidation(boolean debugSkipExpiryValidation) {
            this.debugSkipExpiryValidation = debugSkipExpiryValidation;
        }
    }

    public static class Rules {
        private int maxExtensionDays = 730;
        private int cooldownSeconds = 10;
        private int maxSubscriptionDurationDays = 730;

        public int getMaxExtensionDays() {
            return maxExtensionDays;
        }

        public void setMaxExtensionDays(int maxExtensionDays) {
            this.maxExtensionDays = maxExtensionDays;
        }

        public int getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(int cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }

        public int getMaxSubscriptionDurationDays() {
            return maxSubscriptionDurationDays;
        }

        public void setMaxSubscriptionDurationDays(int maxSubscriptionDurationDays) {
            this.maxSubscriptionDurationDays = maxSubscriptionDurationDays;
        }
    }

    public static class Payment {
        private String serviceUrl;
        private int timeout;
        private Retry retry = new Retry();
        private CircuitBreaker circuitBreaker = new CircuitBreaker();

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public static class Retry {
            private int maxAttempts = 3;
            private int delay = 2000;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public int getDelay() {
                return delay;
            }

            public void setDelay(int delay) {
                this.delay = delay;
            }
        }

        public static class CircuitBreaker {
            private int failureThreshold = 5;
            private int waitDuration = 60000;

            public int getFailureThreshold() {
                return failureThreshold;
            }

            public void setFailureThreshold(int failureThreshold) {
                this.failureThreshold = failureThreshold;
            }

            public int getWaitDuration() {
                return waitDuration;
            }

            public void setWaitDuration(int waitDuration) {
                this.waitDuration = waitDuration;
            }
        }
    }

    public static class Observability {
        private String loggingLevel = "INFO";
        private boolean metricsEnabled = true;
        private boolean tracingEnabled = true;

        public String getLoggingLevel() {
            return loggingLevel;
        }

        public void setLoggingLevel(String loggingLevel) {
            this.loggingLevel = loggingLevel;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isTracingEnabled() {
            return tracingEnabled;
        }

        public void setTracingEnabled(boolean tracingEnabled) {
            this.tracingEnabled = tracingEnabled;
        }
    }

    public static class ThreadPool {
        private int coreSize = 5;
        private int maxSize = 10;
        private int queueCapacity = 100;

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Cache {
        private SubscriptionPlans subscriptionPlans = new SubscriptionPlans();

        public SubscriptionPlans getSubscriptionPlans() {
            return subscriptionPlans;
        }

        public void setSubscriptionPlans(SubscriptionPlans subscriptionPlans) {
            this.subscriptionPlans = subscriptionPlans;
        }

        public static class SubscriptionPlans {
            private int ttl = 3600;
            private boolean enabled = true;

            public int getTtl() {
                return ttl;
            }

            public void setTtl(int ttl) {
                this.ttl = ttl;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    public static class Idempotency {
        private int redisTtlSeconds = 10;

        public int getRedisTtlSeconds() {
            return redisTtlSeconds;
        }

        public void setRedisTtlSeconds(int redisTtlSeconds) {
            this.redisTtlSeconds = redisTtlSeconds;
        }
    }

    public static class Kafka {
        private String topicBookKeeping = "book-keeping-events";

        public String getTopicBookKeeping() {
            return topicBookKeeping;
        }

        public void setTopicBookKeeping(String topicBookKeeping) {
            this.topicBookKeeping = topicBookKeeping;
        }
    }
}

