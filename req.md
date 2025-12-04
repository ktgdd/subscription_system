Functional Requirements

The application hosts multiple subscription plans provided to users. Admin users can create, read, update, and delete subscription plans which serve as catalog templates. Subscription plans contain metadata such as name, description, price, features, and duration type. When updating subscription plans, admins can elongate or shorten the plan duration, but these changes only affect future subscriptions and do not impact existing user subscriptions. Subscription plan deletion is implemented as a soft delete using a flag, not permanent deletion.

Subscription plans support three duration types: weekly, monthly, and yearly. Duration types are stored in the database rather than hardcoded. Admin can configure duration types through a rules engine. Code architecture supports adding new duration types without modification.

A rules engine allows admin users to configure business rules including maximum extension limits with a default of two years, cooldown periods, and other business rules stored in the database.


Users can subscribe to one or many subscription plans, processed one at a time. Users can subscribe to multiple independent subscriptions. When a user subscribes, the system creates a new subscription record with appropriate start and end dates based on the selected plan's duration type.

Users can extend their existing subscription end dates by elongation only, never by shortening. When extending, the system adds time to the existing end date rather than recalculating from the current date. Maximum extension limit is two years, configurable through the rules engine. Cooldown period of ten seconds per user per subscription prevents double-click issues and rapid duplicate requests. Subscription plans in the catalog can be elongated or shortened by admin, but user subscription end dates can only be elongated.

Users can view their active subscriptions and subscription history including start date, end date, status, and other relevant information. Users can only access their own subscription data.

The system tracks subscription lifecycle states: active, cancelled, and expired. Active subscriptions are currently valid. Cancelled subscriptions remain valid until their end date. Expired subscriptions have passed their end date. Status transitions are tracked for audit purposes. The system validates subscription access based on dates. There is no automatic payment or renewal functionality.

Future extensibility features include bundle subscriptions, notification system for subscription expiry and renewals, timezone handling for timezone-aware subscription dates, and proration calculations as informational only.

Assumptions

JWT tokens are provided by an external authentication service with user IDs included. The service runs in a containerized environment with PostgreSQL and Redis available and configured. API Gateway handles initial authentication. This service validates JWT tokens and extracts user and role information.

No automatic payment processing required. Subscription plan deletion is soft delete using a flag-based approach. Users can subscribe multiple times to the same subscription plan, which extends the end date. Ten-second cooldown period per user per subscription prevents rapid duplicate requests.

Maximum subscription extension is two years, configurable through the rules engine. Subscription plans in the catalog can be elongated or shortened by admin users, but user subscription end dates can only be elongated, never shortened.

The service is an internal microservice and does not handle user registration or authentication directly. All subscription operations are transactional and require proper authorization. The system is extensible for future features like bundle subscriptions, notifications, and timezone handling.

Non Functional Requirements

Response time target is under two hundred milliseconds for subscription operations at the ninety-fifth percentile using Redis caching.

System handles five to ten concurrent subscription requests per container using thread pool configuration with no concurrent operations per user.

All subscription availability and discovery data is cached in Redis with write-through cache strategy writing to both database and Redis.

Read operations use Redis with fallback to PostgreSQL implementing passive caching, and subscription catalog loads into Redis at application startup.

PostgreSQL stores transactional data including user subscriptions, subscription records, subscription discovery and catalog data, and rules engine configuration.

Redis stores subscription availability cache, rate limiting data, and session or state data.

Circuit breakers are implemented for external subscription site integrations to prevent cascading failures and enable graceful degradation.

Rate limiters prevent abuse with per-user and per-endpoint rate limits stored in Redis for distributed rate limiting.

Role-based authorization is implemented within the service with JWT tokens containing user and role information validated through middleware.

Admin users have full CRUD capabilities on subscription catalog and can manage rules, while regular users can subscribe and extend subscriptions.

Users can only access their own subscription data with authorization checks implemented in the service layer.

Structured logging includes request and response logging with correlation IDs, business event logging, error logging with stack traces, and JSON format.

Application and business metrics include HTTP request metrics, subscription business metrics, cache hit and miss ratios, database query performance, circuit breaker states, and rate limiter metrics in Prometheus format.

Distributed tracing provides end-to-end request tracing with trace ID propagation, span creation for major operations, and integration with Jaeger and Zipkin.

Health checks include basic application health endpoint, detailed health endpoint showing database and Redis connectivity, and metrics endpoint for Prometheus.

Metrics and logs are compatible with alerting systems for error rate thresholds, performance degradation alerts, circuit breaker state changes, and cache miss alerts.

Strong consistency is maintained for subscription operations with subscription creation and extension operations being atomic using database transactions and cache invalidation.

Write-through cache strategy ensures updates write to both PostgreSQL and Redis with cache invalidation on subscription catalog updates and boot-time cache warm-up.

Technology stack includes Java 21, Spring Boot 4.0.0, PostgreSQL for transactional and catalog data, Redis for availability data and rate limiting, and Maven.

Architecture follows internal microservice pattern with REST API communication, JWT token validation but not generation, and single-container deployment.
