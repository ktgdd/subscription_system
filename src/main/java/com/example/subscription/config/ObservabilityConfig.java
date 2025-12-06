package com.example.subscription.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    // Spring Boot Actuator automatically provides health checks for:
    // - Database (via DataSource)
    // - Redis (via ReactiveRedisConnectionFactory)
    // - Kafka (if configured)
    // Custom health indicators can be added here if needed
}

