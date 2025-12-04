# Database Schema Design

## Overview

This document describes the database schema for the Subscription Service. The schema is designed to support:
- Account-based subscription catalog (Netflix, Amazon, etc.)
- Plan versioning with soft deletes
- Event-driven architecture with book keeping as write-ahead log
- User subscription management with eventual consistency
- Rules engine for admin configuration

## Architecture Principles

1. **Book Keeping as Source of Truth**: All subscription events are written to `book_keeping` first, then materialized to `user_subscriptions`
2. **Account-Based Catalog**: Subscriptions organized by accounts (Netflix, Amazon) with plans (Weekly, Monthly, Yearly)
3. **Plan Versioning**: Only one active plan per account+duration_type, old plans soft-deleted
4. **Eventual Consistency**: Book keeping written synchronously, materialization happens asynchronously
5. **Idempotency**: Prevent duplicate operations via idempotency keys

## Table Relationships

```
subscription_accounts (1) ──< (N) subscription_plans
duration_types (1) ──< (N) subscription_plans
subscription_plans (1) ──< (N) book_keeping
subscription_accounts (1) ──< (N) user_subscriptions
duration_types (1) ──< (N) user_subscriptions
```

## Tables

### 1. subscription_accounts

Stores subscription service providers (Netflix, Amazon, Spotify, etc.)

**Columns:**
- `id`: Primary key
- `name`: Account name (unique, indexed)
- `description`: Account description
- `logo_url`: Optional logo URL
- `is_active`: Whether account is active
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

**Constraints:**
- `name` must be unique
- `name` cannot be null

**Indexes:**
- Primary key on `id`
- Unique index on `name`
- Index on `is_active` for filtering active accounts

### 2. duration_types

Reference data table for subscription duration types (Weekly, Monthly, Yearly)

**Columns:**
- `id`: Primary key
- `type`: Duration type name (WEEKLY, MONTHLY, YEARLY)
- `days`: Number of days for this duration type
- `description`: Description of duration type
- `created_at`: Creation timestamp

**Constraints:**
- `type` must be unique
- `type` cannot be null

**Indexes:**
- Primary key on `id`
- Unique index on `type`

**Initial Data:**
- WEEKLY (7 days)
- MONTHLY (30 days)
- YEARLY (365 days)

### 3. subscription_plans

Stores subscription plans for each account and duration type combination.

**Columns:**
- `id`: Primary key
- `subscription_account_id`: Foreign key to subscription_accounts
- `duration_type_id`: Foreign key to duration_types
- `amount`: Price amount
- `currency`: Currency code (USD, EUR, etc.)
- `name`: Plan name (optional)
- `description`: Plan description
- `features`: JSON field for plan features
- `is_active`: Whether plan is currently active (only one active per account+duration_type)
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp
- `deleted_at`: Soft delete timestamp (NULL if not deleted)

**Constraints:**
- `subscription_account_id` cannot be null
- `duration_type_id` cannot be null
- `amount` must be positive
- `currency` cannot be null
- Business rule: Only one active plan per account+duration_type (enforced at application level)

**Indexes:**
- Primary key on `id`
- Foreign key indexes on `subscription_account_id` and `duration_type_id`
- Composite index on `(subscription_account_id, duration_type_id, is_active, deleted_at)` for querying active plans
- Index on `deleted_at` for soft delete queries

### 4. book_keeping

Write-ahead log for all subscription events. Source of truth for subscription operations.

**Columns:**
- `id`: Primary key
- `idempotency_key`: Unique key to prevent duplicate operations
- `user_id`: User who performed the operation
- `subscription_plan_id`: Foreign key to subscription_plans (historical reference)
- `subscription_account_id`: Denormalized account ID for easier queries
- `duration_type_id`: Denormalized duration type for easier queries
- `event_type`: Type of event (SUBSCRIBED, EXTENDED, CANCELLED, EXPIRED)
- `status`: Processing status (INITIATED, COMPLETED, PROCESSED, FAILED)
- `before_state`: JSON field with state before operation
- `after_state`: JSON field with state after operation
- `payment_reference_id`: Reference ID from payment service
- `created_at`: When event was initiated
- `completed_at`: When payment service marked as completed
- `processed_at`: When event was materialized to user_subscriptions
- `retry_count`: Number of retry attempts
- `error_message`: Error message if processing failed

**Constraints:**
- `idempotency_key` must be unique
- `user_id` cannot be null
- `subscription_plan_id` cannot be null (for historical reference)
- `event_type` cannot be null
- `status` cannot be null

**Indexes:**
- Primary key on `id`
- Unique index on `idempotency_key` (prevents duplicates)
- Index on `user_id` for user-specific queries
- Index on `status` for processing queries (INITIATED, COMPLETED)
- Composite index on `(status, created_at)` for async processor
- Index on `subscription_account_id` and `duration_type_id` for queries

**JSON Structure:**
```json
before_state: {
  "start_date": "2024-01-01",
  "end_date": "2024-02-01",
  "status": "ACTIVE"
}

after_state: {
  "start_date": "2024-01-01",
  "end_date": "2024-03-01",
  "status": "ACTIVE"
}
```

### 5. user_subscriptions

Materialized view of current user subscription state. Derived from book_keeping.

**Columns:**
- `id`: Primary key
- `user_id`: User who owns the subscription
- `subscription_account_id`: Foreign key to subscription_accounts
- `duration_type_id`: Foreign key to duration_types
- `start_date`: Subscription start date
- `end_date`: Subscription end date (calculated from book_keeping)
- `status`: Current status (ACTIVE, CANCELLED, EXPIRED)
- `created_at`: When subscription was created
- `last_updated_at`: Last update timestamp

**Constraints:**
- `user_id` cannot be null
- `subscription_account_id` cannot be null
- `duration_type_id` cannot be null
- `start_date` cannot be null
- `end_date` cannot be null
- `end_date` must be >= `start_date`
- Business rule: Only one active subscription per user+account+duration_type (enforced via unique constraint)

**Indexes:**
- Primary key on `id`
- Foreign key indexes on `subscription_account_id` and `duration_type_id`
- Unique composite index on `(user_id, subscription_account_id, duration_type_id)` WHERE `status = 'ACTIVE'` (prevents multiple active subscriptions)
- Index on `user_id` for user-specific queries
- Index on `status` for filtering active subscriptions
- Composite index on `(user_id, status)` for user's active subscriptions
- Index on `end_date` for expiry queries

### 6. rules_engine

Admin-configurable business rules for subscription management.

**Columns:**
- `id`: Primary key
- `rule_key`: Unique rule identifier
- `rule_name`: Human-readable rule name
- `rule_value`: Rule value (JSON or text)
- `rule_type`: Type of rule (MAX_EXTENSION_DAYS, COOLDOWN_SECONDS, etc.)
- `description`: Rule description
- `is_active`: Whether rule is active
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

**Constraints:**
- `rule_key` must be unique
- `rule_key` cannot be null
- `rule_type` cannot be null

**Indexes:**
- Primary key on `id`
- Unique index on `rule_key`
- Index on `is_active` for active rules

**Default Rules:**
- `MAX_EXTENSION_DAYS`: 730 (2 years)
- `COOLDOWN_SECONDS`: 10
- `MAX_SUBSCRIPTION_DURATION_DAYS`: 730

## Data Flow

### Subscribe Flow
1. Write to `book_keeping` with status=INITIATED
2. Payment service processes payment
3. Update `book_keeping` to status=COMPLETED
4. Async processor materializes to `user_subscriptions`
5. Update `book_keeping` to status=PROCESSED

### Extend Flow
1. Write to `book_keeping` with event_type=EXTENDED, status=INITIATED
2. Payment service processes payment
3. Update `book_keeping` to status=COMPLETED
4. Async processor updates `user_subscriptions.end_date`
5. Update `book_keeping` to status=PROCESSED

## Idempotency Key Format

```
{user_id}:{subscription_account_id}:{duration_type_id}:{request_id}
```

Where:
- `request_id`: Extracted from JWT claim "request_id" or generated UUID
- Stored in Redis with 10-second TTL (cooldown period)
- Also stored in `book_keeping.idempotency_key` with UNIQUE constraint

## Query Patterns

### Get Active Plans for Account
```sql
SELECT * FROM subscription_plans
WHERE subscription_account_id = ?
  AND is_active = true
  AND deleted_at IS NULL;
```

### Get User's Active Subscriptions
```sql
SELECT * FROM user_subscriptions
WHERE user_id = ?
  AND status = 'ACTIVE'
  AND end_date > NOW();
```

### Get Pending Book Keeping Entries
```sql
SELECT * FROM book_keeping
WHERE status = 'COMPLETED'
  AND processed_at IS NULL
ORDER BY completed_at ASC;
```

### Check Subscription Validity
```sql
SELECT * FROM user_subscriptions
WHERE user_id = ?
  AND subscription_account_id = ?
  AND duration_type_id = ?
  AND status = 'ACTIVE'
  AND end_date > NOW();
```

## Performance Considerations

1. **Indexes**: All foreign keys and frequently queried columns are indexed
2. **Composite Indexes**: Optimized for common query patterns
3. **Soft Deletes**: Use `deleted_at IS NULL` instead of hard deletes
4. **Denormalization**: `subscription_account_id` and `duration_type_id` in `book_keeping` for faster queries
5. **JSON Fields**: Used for flexible state storage, indexed via PostgreSQL JSONB if needed

## Migration Strategy

1. Create tables in order: accounts → duration_types → plans → book_keeping → user_subscriptions → rules
2. Insert initial duration_types data
3. Insert default rules_engine data
4. Create indexes after table creation for better performance

## Future Considerations

1. **Partitioning**: `book_keeping` table can be partitioned by date for better performance
2. **Archiving**: Old `book_keeping` entries can be archived to separate tables
3. **Read Replicas**: For read-heavy operations on `user_subscriptions`
4. **Caching**: Subscription catalog cached in Redis (as per requirements)

