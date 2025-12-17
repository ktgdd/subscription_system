# Database Schema Analysis

## Overview
The schema implements an **event-sourcing pattern** with **eventual consistency** for a subscription management system. It separates the write-ahead log (book_keeping) from the materialized view (user_subscriptions).

---

## Table Structure Analysis

### 1. **subscription_accounts** (Reference Data)
**Purpose:** Master data for subscription providers (Netflix, Amazon, Spotify)

**Design Quality:** ✅ Excellent
- Simple, normalized structure
- Proper soft-delete capability with `is_active`
- Audit trail with timestamps
- Unique constraint on name prevents duplicates

**Potential Issues:** None identified

---

### 2. **duration_types** (Reference Data)
**Purpose:** Catalog of subscription duration types (WEEKLY, MONTHLY, YEARLY)

**Design Quality:** ✅ Excellent
- Immutable reference data
- Days stored explicitly (good for calculations)
- Pre-populated with standard values

**Potential Issues:** None identified

---

### 3. **subscription_plans** (Catalog Data)
**Purpose:** Pricing plans for each account + duration combination

**Design Quality:** ✅ Very Good
- **Business Rule:** Only one active plan per (account_id, duration_type_id)
- Soft delete with `deleted_at` preserves history
- JSONB for flexible features storage
- Proper foreign keys with RESTRICT (prevents orphaned records)

**Key Design Decisions:**
- ✅ Stores `subscription_account_id` (not plan_id) in user_subscriptions - survives plan versioning
- ✅ Soft delete allows historical reference
- ⚠️ **Enforced at application level** - no unique constraint in DB

**Potential Issues:**
- ⚠️ **Missing unique constraint** for active plans - relies on application logic
- ⚠️ **No versioning mechanism** - if admin wants to track plan history, need separate version table
- ✅ JSONB for features is flexible but requires careful validation

**Recommendation:**
```sql
-- Consider adding partial unique index
CREATE UNIQUE INDEX idx_subscription_plans_unique_active 
    ON subscription_plans(subscription_account_id, duration_type_id) 
    WHERE is_active = true AND deleted_at IS NULL;
```

---

### 4. **book_keeping** (Event Store - Source of Truth)
**Purpose:** Write-ahead log for all subscription events

**Design Quality:** ✅ Excellent - Implements Event Sourcing Pattern

**Key Features:**
- ✅ **Idempotency key** (unique) - prevents duplicate processing
- ✅ **Event types:** SUBSCRIBED, EXTENDED, CANCELLED, EXPIRED
- ✅ **Status flow:** INITIATED → COMPLETED → PROCESSED → FAILED
- ✅ **Before/After state** (JSONB) - full audit trail
- ✅ **Denormalized fields** (account_id, duration_type_id) - improves query performance
- ✅ **Retry mechanism** with retry_count and error_message

**Design Patterns:**
1. **Event Sourcing:** All events stored, can replay history
2. **Idempotency:** Unique key prevents duplicate processing
3. **Audit Trail:** Complete before/after state tracking
4. **Async Processing:** Status transitions show async flow

**Potential Issues:**
- ⚠️ **No partitioning strategy** - table will grow large over time
- ⚠️ **JSONB state** - no schema validation, could have inconsistent data
- ✅ Good indexing strategy for common queries

**Recommendation:**
- Consider partitioning by `created_at` for large-scale deployments
- Add JSON schema validation for before_state/after_state
- Consider archiving old PROCESSED records

---

### 5. **user_subscriptions** (Materialized View - Current State)
**Purpose:** Current state of user subscriptions, materialized from book_keeping

**Design Quality:** ✅ Excellent - Implements CQRS Read Model

**Key Features:**
- ✅ **References account_id, not plan_id** - survives plan deletion/versioning
- ✅ **Unique constraint** on active subscriptions per (user_id, account_id, duration_type_id)
- ✅ **End date can only be elongated** - enforced at application level
- ✅ **Status tracking:** ACTIVE, CANCELLED, EXPIRED

**Design Patterns:**
1. **CQRS:** Separate read model from write model
2. **Eventual Consistency:** Updated asynchronously from book_keeping
3. **Denormalization:** Stores account_id for fast queries

**Potential Issues:**
- ⚠️ **No foreign key to subscription_plans** - intentional (survives plan deletion)
- ⚠️ **End date elongation** - enforced at app level, not DB constraint
- ✅ Good unique constraint with partial index

**Recommendation:**
- Consider adding check constraint: `CHECK (end_date >= start_date)` (already in SQL)
- Consider adding index on `(user_id, status, end_date)` for expiry queries

---

### 6. **rules_engine** (Configuration)
**Purpose:** Admin-configurable business rules

**Design Quality:** ✅ Good

**Key Features:**
- ✅ Flexible key-value storage
- ✅ Rule types for categorization
- ✅ Active/inactive flag
- ✅ JSON support for complex rules

**Potential Issues:**
- ⚠️ **No validation** - rule_value is TEXT, could be invalid
- ⚠️ **No versioning** - can't track rule changes over time
- ✅ Good for simple rules, may need enhancement for complex logic

---

## Relationship Analysis

### Foreign Key Relationships
```
subscription_accounts (1) ──< (N) subscription_plans
duration_types (1) ──< (N) subscription_plans
subscription_plans (1) ──< (N) book_keeping
subscription_accounts (1) ──< (N) book_keeping
duration_types (1) ──< (N) book_keeping
subscription_accounts (1) ──< (N) user_subscriptions
duration_types (1) ──< (N) user_subscriptions
```

**All relationships use `ON DELETE RESTRICT`** - ✅ Good, prevents accidental data loss

---

## Design Patterns Identified

### 1. **Event Sourcing** ✅
- `book_keeping` is the event store
- All changes tracked as events
- Can replay events to rebuild state

### 2. **CQRS (Command Query Responsibility Segregation)** ✅
- Write: `book_keeping` (event store)
- Read: `user_subscriptions` (materialized view)
- Async materialization via Kafka

### 3. **Eventual Consistency** ✅
- Book keeping written synchronously
- User subscriptions updated asynchronously
- Status transitions show the flow

### 4. **Idempotency** ✅
- Unique `idempotency_key` in book_keeping
- Also checked in Redis (10s TTL)
- Prevents duplicate processing

### 5. **Soft Delete** ✅
- `subscription_plans.deleted_at`
- `subscription_accounts.is_active`
- Preserves history

---

## Business Rules Analysis

### ✅ Enforced Rules

1. **Only one active plan per (account_id, duration_type_id)**
   - Enforced at application level
   - ⚠️ Should add DB constraint for safety

2. **Only one active subscription per (user_id, account_id, duration_type_id)**
   - ✅ Enforced with unique partial index

3. **End date can only be elongated**
   - Enforced at application level
   - ⚠️ Could add trigger for extra safety

4. **Idempotency**
   - ✅ Unique constraint on idempotency_key
   - ✅ Redis check with TTL

5. **Plan versioning**
   - ✅ Stores account_id in user_subscriptions (not plan_id)
   - ✅ Survives plan deletion/updates

---

## Potential Issues & Recommendations

### 🔴 Critical Issues
None identified

### 🟡 Medium Priority

1. **Missing Unique Constraint for Active Plans**
   ```sql
   -- Add to schema.sql
   CREATE UNIQUE INDEX idx_subscription_plans_unique_active 
       ON subscription_plans(subscription_account_id, duration_type_id) 
       WHERE is_active = true AND deleted_at IS NULL;
   ```

2. **No Partitioning Strategy for book_keeping**
   - Table will grow large over time
   - Consider partitioning by `created_at` (monthly/quarterly)

3. **JSONB Validation**
   - No schema validation for before_state/after_state
   - Consider adding check constraints or application-level validation

### 🟢 Low Priority / Enhancements

1. **Rules Engine Validation**
   - Add validation for rule_value based on rule_type
   - Consider versioning for rule changes

2. **Index Optimization**
   - Consider composite indexes for common query patterns
   - Review index usage with EXPLAIN ANALYZE

3. **Archiving Strategy**
   - Plan for archiving old PROCESSED book_keeping records
   - Consider separate archive table

---

## Alignment with Code Implementation

### ✅ Matches Code
- All entity classes match schema
- Foreign key relationships correct
- Column names match
- Data types match

### ⚠️ Potential Mismatches

1. **UserSubscription.lastUpdatedAt**
   - Schema: `last_updated_at`
   - Entity: `lastUpdatedAt` ✅ (JPA handles snake_case)

2. **BookKeeping.idempotencyKey**
   - Schema: `idempotency_key`
   - Entity: `idempotencyKey` ✅ (JPA handles snake_case)

---

## Performance Considerations

### ✅ Good Indexing
- Primary keys on all tables
- Foreign key indexes
- Unique constraints with indexes
- Partial indexes for active records

### 📊 Query Patterns Supported
1. ✅ Get active plans by account
2. ✅ Get user subscriptions
3. ✅ Find book keeping by idempotency key
4. ✅ Query by status
5. ✅ Find subscriptions expiring soon

### ⚠️ Potential Bottlenecks
1. **book_keeping table growth** - needs partitioning
2. **JSONB queries** - may be slower than structured columns
3. **Materialization lag** - eventual consistency means slight delay

---

## Summary

### Strengths ✅
1. **Excellent event sourcing implementation**
2. **Proper separation of concerns** (event store vs. materialized view)
3. **Good indexing strategy**
4. **Idempotency built-in**
5. **Survives plan versioning** (stores account_id, not plan_id)
6. **Audit trail** with before/after states

### Areas for Improvement ⚠️
1. Add unique constraint for active plans
2. Consider partitioning for book_keeping
3. Add JSONB validation
4. Plan for archiving old records

### Overall Assessment: **Excellent Design** ✅
The schema demonstrates sophisticated understanding of:
- Event sourcing
- CQRS
- Eventual consistency
- Idempotency
- Soft deletes
- Denormalization for performance

This is a **production-ready schema** with minor enhancements recommended for scale.


