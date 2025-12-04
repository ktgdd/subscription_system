# Subscription Service - Requirements Document

## 1. Project Overview

This is an internal microservice responsible for managing subscription catalog and user subscriptions. The service provides role-based access control and is designed to be scalable, observable, and performant.

### 1.1 Key Terminology Distinction

**Important**: There are two distinct concepts in this system:

1. **Subscription Plans** (Catalog/Templates):
   - Managed by Admin users
   - Define available subscription types (Weekly, Monthly, Yearly)
   - Can be **elongated OR shortened** by admin
   - Changes to plans do NOT affect existing user subscriptions
   - Stored in subscription catalog

2. **User Subscriptions** (Actual User Instances):
   - Created when users subscribe to a plan
   - Tied to specific users with start/end dates
   - End dates can **ONLY be elongated, NOT shortened**
   - Protected from reduction to maintain user commitments

## 2. Functional Requirements

### 2.1 Subscription Catalog Management

#### FR-1.1: Subscription Plan CRUD Operations
- **Description**: Admin users can create, read, update, and delete subscription plans (catalog/templates)
- **Details**:
  - Create subscription plans with metadata (name, description, price, features, duration type, etc.)
  - Update existing subscription plans (can be elongated OR shortened - affects future subscriptions only)
  - **Important**: Changes to subscription plans do NOT affect existing user subscriptions
  - Soft delete subscription plans (flag-based, not permanent deletion)
  - View all subscription plans (including deleted ones for historical data)
- **Roles**: Admin only
- **Priority**: P0 (Must Have)

#### FR-1.2: Subscription Duration Types
- **Description**: Subscriptions support three duration types: Weekly, Monthly, Yearly
- **Details**:
  - Duration types stored in database (not hardcoded)
  - Admin can configure duration types via rules engine
  - Code should be extensible for future duration types
- **Storage**: Database schema with duration configuration table
- **Priority**: P0 (Must Have)

#### FR-1.3: Rules Engine for Admin Configuration
- **Description**: Admin-configurable rules for subscription management
- **Details**:
  - Maximum extension limit (default: 2 years, configurable)
  - Cooldown periods
  - Business rules stored in database
- **Priority**: P0 (Must Have)

### 2.2 User Subscription Management

#### FR-2.1: Subscribe to Subscriptions
- **Description**: Users can subscribe to one or many subscriptions, one at a time
- **Details**:
  - Users can subscribe to multiple independent subscriptions
  - Each subscription is independent (bundle support in future iteration)
  - Subscription creates a new subscription record with start/end dates
- **Roles**: User role
- **Priority**: P0 (Must Have)

#### FR-2.2: Extend Existing User Subscriptions
- **Description**: Users can extend their existing subscription end dates
- **Details**:
  - **User subscription end dates can ONLY be elongated, NOT shortened**
  - Extending adds time to the end date (not recalculated from now)
  - Maximum extension limit: 2 years (configurable via rules engine)
  - Cooldown: 10 seconds per user per subscription (prevents double-click issues)
  - **Important Distinction**: 
    - Subscription PLANS (catalog) can be elongated/shortened by admin
    - User SUBSCRIPTIONS (actual user instances) end dates can only be elongated
  - Decision pending: Duplicate entry vs extend date (to be determined in design phase)
- **Roles**: User role
- **Priority**: P0 (Must Have)

#### FR-2.3: View Subscriptions
- **Description**: Users can view their active and historical subscriptions
- **Details**:
  - View all active subscriptions
  - View subscription history
  - View subscription details (start date, end date, status, etc.)
- **Roles**: User role
- **Priority**: P0 (Must Have)

### 2.3 Subscription Lifecycle

#### FR-3.1: Subscription Status Management
- **Description**: Track subscription lifecycle states
- **Details**:
  - Active: Subscription is currently valid
  - Cancelled: Subscription is cancelled but remains valid until end date
  - Expired: Subscription has passed end date
  - Status transitions are tracked for audit purposes
- **Priority**: P0 (Must Have)

#### FR-3.2: Subscription Validation
- **Description**: Validate subscription access based on dates
- **Details**:
  - Cancelled subscriptions remain valid until end date
  - No automatic payment/ renewal (out of scope)
  - System validates subscription access based on current date vs end date
- **Priority**: P0 (Must Have)

### 2.4 Future Extensibility Features (Out of Scope - Documented for Future)

#### FR-4.1: Bundle Subscriptions
- **Description**: Support for bundled subscriptions (multiple subscriptions as a package)
- **Status**: Future iteration
- **Priority**: P2 (Future)

#### FR-4.2: Notification System
- **Description**: Notify users about subscription expiry, renewals, etc.
- **Status**: Last iteration (if time permits)
- **Priority**: P2 (Future)

#### FR-4.3: Timezone Handling
- **Description**: Handle timezone-aware subscription dates
- **Status**: Last iteration, but code should be extensible
- **Priority**: P2 (Future)

#### FR-4.4: Proration Calculations
- **Description**: Calculate prorated amounts for subscription changes
- **Status**: Out of scope (no automatic payment)
- **Note**: May be informational only if implemented
- **Priority**: P3 (Nice to Have)

## 3. Non-Functional Requirements

### 3.1 Performance Requirements

#### NFR-1.1: Response Time
- **Target**: Sub-200ms for subscription operations (p95)
- **Strategy**: Redis caching for subscription availability data
- **Priority**: P0

#### NFR-1.2: Throughput
- **Target**: 5-10 concurrent subscription requests per container
- **Strategy**: Thread pool configuration for request handling
- **Implementation**: No concurrent subscription operations per user (prevents race conditions)
- **Priority**: P0

### 3.2 Scalability Requirements

#### NFR-2.1: Caching Strategy
- **Description**: Multi-layer caching for performance
- **Details**:
  - **Redis**: All subscription availability/discovery data cached
  - **Write-through cache**: Updates to subscriptions write to both DB and Redis
  - **Passive caching**: Read operations use Redis, fallback to PostgreSQL
  - **Boot-time loading**: Subscription catalog loaded into Redis at application startup
- **Priority**: P0

#### NFR-2.2: Database Strategy
- **PostgreSQL**: 
  - Transactional data (user subscriptions, subscription records)
  - Subscription discovery/catalog data
  - Rules engine configuration
- **Redis**:
  - Subscription availability cache
  - Rate limiting data
  - Session/state data
- **Priority**: P0

### 3.3 Reliability Requirements

#### NFR-3.1: Circuit Breakers
- **Description**: Implement circuit breakers for external subscription site integrations
- **Purpose**: Prevent cascading failures, graceful degradation
- **Priority**: P0

#### NFR-3.2: Rate Limiting
- **Description**: Implement rate limiters to prevent abuse
- **Details**:
  - Per-user rate limits
  - Per-endpoint rate limits
  - Stored in Redis for distributed rate limiting
- **Priority**: P0

### 3.4 Security Requirements

#### NFR-4.1: Authentication
- **Description**: Authentication handled by external service (API Gateway/Auth Service)
- **Status**: Out of scope for this service
- **Note**: Service receives JWT tokens with user/role information

#### NFR-4.2: Authorization
- **Description**: Role-based authorization within the service
- **Roles**:
  - **Admin**: Full CRUD on subscription catalog, manage rules
  - **User**: Subscribe, view own subscriptions, extend subscriptions
- **Implementation**: JWT token validation and role extraction middleware
- **Priority**: P0

#### NFR-4.3: Data Isolation
- **Description**: Users can only access their own subscription data
- **Implementation**: Authorization checks in service layer
- **Priority**: P0

### 3.5 Observability Requirements (KEY FOCUS - DevOps AI Org)

#### NFR-5.1: Logging
- **Description**: Comprehensive structured logging
- **Details**:
  - Request/response logging with correlation IDs
  - Business event logging (subscription created, extended, cancelled)
  - Error logging with stack traces and context
  - Log levels: DEBUG, INFO, WARN, ERROR
  - Structured format (JSON) for log aggregation
- **Priority**: P0

#### NFR-5.2: Metrics
- **Description**: Application and business metrics
- **Details**:
  - HTTP request metrics (count, duration, status codes)
  - Business metrics (subscriptions created, extended, active count)
  - Cache hit/miss ratios
  - Database query performance
  - Circuit breaker states
  - Rate limiter metrics
- **Format**: Prometheus-compatible metrics
- **Priority**: P0

#### NFR-5.3: Distributed Tracing
- **Description**: End-to-end request tracing
- **Details**:
  - Trace ID propagation across service calls
  - Span creation for major operations
  - Integration with tracing systems (Jaeger, Zipkin compatible)
- **Priority**: P0

#### NFR-5.4: Health Checks
- **Description**: Application health and readiness endpoints
- **Details**:
  - `/health`: Basic application health
  - `/actuator/health`: Detailed health (database, Redis connectivity)
  - `/metrics`: Prometheus metrics endpoint
- **Priority**: P0

#### NFR-5.5: Alerting Integration
- **Description**: Metrics and logs should be compatible with alerting systems
- **Details**:
  - Error rate thresholds
  - Performance degradation alerts
  - Circuit breaker state changes
  - Cache miss rate alerts
- **Priority**: P1

### 3.6 Data Consistency Requirements

#### NFR-6.1: Transactional Integrity
- **Description**: Strong consistency for subscription operations
- **Details**:
  - Subscription creation/extension must be atomic
  - Database transactions for critical operations
  - Cache invalidation on writes
- **Priority**: P0

#### NFR-6.2: Cache Consistency
- **Description**: Write-through cache strategy
- **Details**:
  - Updates write to both PostgreSQL and Redis
  - Cache invalidation on subscription catalog updates
  - Boot-time cache warm-up
- **Priority**: P0

## 4. Technical Constraints

### 4.1 Technology Stack
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.0
- **Database**: PostgreSQL (transactional + catalog data)
- **Cache**: Redis (availability data, rate limiting, state)
- **Build Tool**: Maven

### 4.2 Architecture Constraints
- **Pattern**: Internal microservice
- **Communication**: REST API
- **Authentication**: JWT tokens (validated, not generated)
- **Deployment**: Single-container deployment (not multi-region for demo)

## 5. Out of Scope (Explicitly Excluded)

1. **User Authentication**: Handled by external service
2. **Automatic Payment**: No payment processing
3. **Automatic Renewal**: No auto-renewal functionality
4. **Multi-region Deployment**: Single region for demonstration
5. **User Registration**: Not part of this service
6. **Payment Gateway Integration**: Out of scope
7. **Email/SMS Notifications**: Future iteration only

## 6. Assumptions

1. JWT tokens are provided by external authentication service
2. User IDs are included in JWT tokens
3. Service runs in containerized environment
4. PostgreSQL and Redis are available and configured
5. API Gateway handles initial authentication
6. No automatic payment processing required
7. Subscription plan deletion is soft delete (flag-based)
8. Users can subscribe multiple times to same subscription plan (extends end date)
9. 10-second cooldown per user per subscription prevents rapid duplicate requests
10. Maximum subscription extension: 2 years (configurable)
11. **Key Distinction**: Subscription plans can be elongated/shortened, but user subscription end dates can only be elongated

## 7. Open Design Decisions (To Be Resolved)

1. **Subscription Extension Strategy**: 
   - Option A: Create duplicate entry with extended end date
   - Option B: Update existing entry's end date
   - **Decision**: To be made in design phase

2. **Proration Implementation**: 
   - Informational only vs. full calculation
   - **Decision**: Out of scope for now, extensible design

3. **Timezone Handling**: 
   - UTC vs. user timezone
   - **Decision**: Last iteration, code should be extensible

## 8. Success Criteria

1. Admin can create/update/delete subscription catalog
2. Users can subscribe to multiple subscriptions
3. Users can extend existing subscriptions with cooldown protection
4. System validates subscription access based on dates
5. All subscription data cached in Redis for performance
6. Comprehensive observability (logs, metrics, traces)
7. Role-based authorization working correctly
8. Circuit breakers and rate limiters functional
9. Response times under 200ms (p95)
10. Health checks and metrics endpoints available

---

**Document Version**: 1.0  
**Last Updated**: [Current Date]  
**Author**: Development Team  
**Status**: Draft - Pending Design Review

