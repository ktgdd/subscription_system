-- Subscription Service Database Schema
-- Version: 1.0
-- Created: 2024

-- 1. SUBSCRIPTION ACCOUNTS

CREATE TABLE subscription_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_accounts_name ON subscription_accounts(name);
CREATE INDEX idx_subscription_accounts_is_active ON subscription_accounts(is_active);

COMMENT ON TABLE subscription_accounts IS 'Stores subscription service providers like Netflix, Amazon, etc.';
COMMENT ON COLUMN subscription_accounts.name IS 'Unique account name';
COMMENT ON COLUMN subscription_accounts.is_active IS 'Whether account is currently active';

-- 2. DURATION TYPES

CREATE TABLE duration_types (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    days INTEGER NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_duration_types_type ON duration_types(type);

COMMENT ON TABLE duration_types IS 'Reference data for subscription duration types';
COMMENT ON COLUMN duration_types.type IS 'Duration type: WEEKLY, MONTHLY, YEARLY';
COMMENT ON COLUMN duration_types.days IS 'Number of days for this duration type';

-- Insert initial duration types
INSERT INTO duration_types (type, days, description) VALUES
    ('WEEKLY', 7, 'Weekly subscription - 7 days'),
    ('MONTHLY', 30, 'Monthly subscription - 30 days'),
    ('YEARLY', 365, 'Yearly subscription - 365 days');

-- 3. SUBSCRIPTION PLANS

CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    subscription_account_id BIGINT NOT NULL,
    duration_type_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    name VARCHAR(255),
    description TEXT,
    features JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    CONSTRAINT fk_subscription_plans_account 
        FOREIGN KEY (subscription_account_id) 
        REFERENCES subscription_accounts(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_subscription_plans_duration_type 
        FOREIGN KEY (duration_type_id) 
        REFERENCES duration_types(id) 
        ON DELETE RESTRICT
);

CREATE INDEX idx_subscription_plans_account_id ON subscription_plans(subscription_account_id);
CREATE INDEX idx_subscription_plans_duration_type_id ON subscription_plans(duration_type_id);
CREATE INDEX idx_subscription_plans_active ON subscription_plans(subscription_account_id, duration_type_id, is_active, deleted_at);
CREATE INDEX idx_subscription_plans_deleted_at ON subscription_plans(deleted_at);

-- 4. BOOK KEEPING
-- Write-ahead log for all subscription events (source of truth)

CREATE TABLE book_keeping (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    subscription_plan_id BIGINT NOT NULL,
    subscription_account_id BIGINT NOT NULL,
    duration_type_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    payment_reference_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    
    CONSTRAINT fk_book_keeping_plan 
        FOREIGN KEY (subscription_plan_id) 
        REFERENCES subscription_plans(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_book_keeping_account 
        FOREIGN KEY (subscription_account_id) 
        REFERENCES subscription_accounts(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_book_keeping_duration_type 
        FOREIGN KEY (duration_type_id) 
        REFERENCES duration_types(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_book_keeping_event_type 
        CHECK (event_type IN ('SUBSCRIBED', 'EXTENDED', 'CANCELLED', 'EXPIRED')),
    
    CONSTRAINT chk_book_keeping_status 
        CHECK (status IN ('INITIATED', 'COMPLETED', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_book_keeping_idempotency_key ON book_keeping(idempotency_key);
CREATE INDEX idx_book_keeping_user_id ON book_keeping(user_id);
CREATE INDEX idx_book_keeping_status ON book_keeping(status);
CREATE INDEX idx_book_keeping_status_created ON book_keeping(status, created_at);
CREATE INDEX idx_book_keeping_account_id ON book_keeping(subscription_account_id);
CREATE INDEX idx_book_keeping_duration_type_id ON book_keeping(duration_type_id);
CREATE INDEX idx_book_keeping_completed_at ON book_keeping(completed_at) WHERE completed_at IS NOT NULL;


-- 5. USER SUBSCRIPTIONS

CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_account_id BIGINT NOT NULL,
    duration_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_subscriptions_account 
        FOREIGN KEY (subscription_account_id) 
        REFERENCES subscription_accounts(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_user_subscriptions_duration_type 
        FOREIGN KEY (duration_type_id) 
        REFERENCES duration_types(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_user_subscriptions_end_date 
        CHECK (end_date >= start_date),
    
    CONSTRAINT chk_user_subscriptions_status 
        CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
);

-- Unique constraint: Only one active subscription per user+account+duration_type
CREATE UNIQUE INDEX idx_user_subscriptions_unique_active 
    ON user_subscriptions(user_id, subscription_account_id, duration_type_id) 
    WHERE status = 'ACTIVE';

CREATE INDEX idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_account_id ON user_subscriptions(subscription_account_id);
CREATE INDEX idx_user_subscriptions_duration_type_id ON user_subscriptions(duration_type_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);
CREATE INDEX idx_user_subscriptions_user_status ON user_subscriptions(user_id, status);
CREATE INDEX idx_user_subscriptions_end_date ON user_subscriptions(end_date);


-- ============================================================================
-- 6. RULES ENGINE
-- ============================================================================
-- Admin-configurable business rules

CREATE TABLE rules_engine (
    id BIGSERIAL PRIMARY KEY,
    rule_key VARCHAR(100) NOT NULL UNIQUE,
    rule_name VARCHAR(255) NOT NULL,
    rule_value TEXT NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rules_engine_rule_key ON rules_engine(rule_key);
CREATE INDEX idx_rules_engine_is_active ON rules_engine(is_active);


-- Insert default rules
INSERT INTO rules_engine (rule_key, rule_name, rule_value, rule_type, description, is_active) VALUES
    ('MAX_EXTENSION_DAYS', 'Maximum Extension Days', '730', 'MAX_EXTENSION_DAYS', 'Maximum number of days a subscription can be extended (2 years)', true),
    ('COOLDOWN_SECONDS', 'Cooldown Period', '10', 'COOLDOWN_SECONDS', 'Cooldown period in seconds to prevent duplicate requests', true),
    ('MAX_SUBSCRIPTION_DURATION_DAYS', 'Maximum Subscription Duration', '730', 'MAX_SUBSCRIPTION_DURATION_DAYS', 'Maximum total subscription duration in days', true);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_subscription_accounts_updated_at 
    BEFORE UPDATE ON subscription_accounts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscription_plans_updated_at 
    BEFORE UPDATE ON subscription_plans 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_subscriptions_updated_at 
    BEFORE UPDATE ON user_subscriptions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rules_engine_updated_at 
    BEFORE UPDATE ON rules_engine 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS (Optional - for easier querying)
-- ============================================================================

-- View for active subscription plans with account and duration type details
CREATE OR REPLACE VIEW v_active_subscription_plans AS
SELECT 
    sp.id,
    sp.subscription_account_id,
    sa.name AS account_name,
    sp.duration_type_id,
    dt.type AS duration_type,
    dt.days AS duration_days,
    sp.amount,
    sp.currency,
    sp.name AS plan_name,
    sp.description,
    sp.features,
    sp.created_at,
    sp.updated_at
FROM subscription_plans sp
INNER JOIN subscription_accounts sa ON sp.subscription_account_id = sa.id
INNER JOIN duration_types dt ON sp.duration_type_id = dt.id
WHERE sp.is_active = true 
  AND sp.deleted_at IS NULL
  AND sa.is_active = true;

COMMENT ON VIEW v_active_subscription_plans IS 'View of all active subscription plans with account and duration details';

