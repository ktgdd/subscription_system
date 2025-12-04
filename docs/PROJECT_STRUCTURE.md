# Project Structure

## Package Organization

```
com.example.subscription/
├── SubscriptionApplication.java          # Main application class
│
├── config/                               # Configuration classes
│   ├── DatabaseConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   ├── ObservabilityConfig.java
│   └── AppProperties.java                # Reads from application.properties
│
├── controller/                           # REST Controllers
│   ├── SubscriptionPlanController.java  # Admin: CRUD plans
│   ├── UserSubscriptionController.java   # User: subscribe, extend, view
│   └── HealthController.java
│
├── service/                              # Business Logic Layer
│   ├── SubscriptionPlanService.java
│   ├── UserSubscriptionService.java
│   ├── BookKeepingService.java
│   └── PaymentService.java
│
├── repository/                          # Data Access Layer
│   ├── SubscriptionAccountRepository.java
│   ├── SubscriptionPlanRepository.java
│   ├── DurationTypeRepository.java
│   ├── BookKeepingRepository.java
│   ├── UserSubscriptionRepository.java
│   └── RulesEngineRepository.java
│
├── model/                                # JPA Entities
│   ├── SubscriptionAccount.java
│   ├── SubscriptionPlan.java
│   ├── DurationType.java
│   ├── BookKeeping.java
│   ├── UserSubscription.java
│   └── RulesEngine.java
│
├── dto/                                  # Data Transfer Objects
│   ├── request/
│   │   ├── CreateSubscriptionPlanRequest.java
│   │   ├── UpdateSubscriptionPlanRequest.java
│   │   ├── SubscribeRequest.java
│   │   └── ExtendSubscriptionRequest.java
│   └── response/
│       ├── SubscriptionPlanResponse.java
│       ├── UserSubscriptionResponse.java
│       └── ApiResponse.java
│
├── exception/                           # Exception Handling
│   ├── GlobalExceptionHandler.java
│   ├── SubscriptionException.java
│   ├── DuplicateRequestException.java
│   └── ValidationException.java
│
├── util/                                 # Utilities
│   ├── IdempotencyKeyGenerator.java
│   ├── DateCalculator.java
│   └── RequestIdExtractor.java
│
├── middleware/                          # Middleware/Interceptors
│   ├── JwtAuthInterceptor.java
│   ├── RoleAuthorizationInterceptor.java
│   └── RequestLoggingInterceptor.java
│
└── cache/                                # Caching Layer
    ├── SubscriptionPlanCache.java
    └── RedisCacheService.java
```

## Design Patterns

1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic separation
3. **DTO Pattern**: Data transfer objects for API
4. **Factory Pattern**: For creating entities from DTOs
5. **Strategy Pattern**: For different subscription calculations
6. **Interceptor Pattern**: For authentication/authorization

## Principles

- **KISS**: Keep It Simple, Stupid - simple interfaces, clear abstractions
- **Single Responsibility**: Each class has one reason to change
- **Dependency Injection**: Spring's DI for loose coupling
- **Configuration Externalization**: All configs in application.properties
- **Layered Architecture**: Clear separation of concerns

## Configuration Management

All configuration values read from `application.properties`:
- Database connection
- Redis connection
- JWT settings
- Business rules (defaults)
- Observability settings

Read on application startup via `@ConfigurationProperties` in `AppProperties` class.

