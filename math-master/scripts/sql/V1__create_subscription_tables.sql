-- =============================================================
-- Migration: create subscription_plans and user_subscriptions
-- Compatible with: PostgreSQL (Neon or local)
-- Idempotent: uses CREATE TABLE IF NOT EXISTS
-- =============================================================

BEGIN;

-- ---------------------------------------------------------------
-- 1) subscription_plans
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_plans (
    plan_id       UUID        NOT NULL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    price         NUMERIC(15, 2),                           -- NULL = contact-sales (enterprise)
    currency      VARCHAR(10)  NOT NULL DEFAULT 'VND',
    billing_cycle VARCHAR(20)  NOT NULL,                    -- enum: FOREVER | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM
    description   TEXT,
    is_featured   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_public     BOOLEAN      NOT NULL DEFAULT TRUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- enum: ACTIVE | INACTIVE
    features      JSONB,                                    -- JSON array of strings
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    UUID,
    deleted_at    TIMESTAMPTZ,
    deleted_by    UUID,
    CONSTRAINT uq_subscription_plans_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS idx_subscription_plans_status
    ON subscription_plans (status)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------
-- 2) user_subscriptions
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_subscriptions (
    subscription_id UUID         NOT NULL PRIMARY KEY,
    user_id         UUID         NOT NULL,
    plan_id         UUID         NOT NULL REFERENCES subscription_plans (plan_id),
    start_date      TIMESTAMPTZ  NOT NULL,
    end_date        TIMESTAMPTZ,                            -- NULL for FOREVER plans
    amount          NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'VND',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- enum: ACTIVE | EXPIRED | CANCELLED
    payment_method  VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id
    ON user_subscriptions (user_id);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_plan_id
    ON user_subscriptions (plan_id);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status
    ON user_subscriptions (status)
    WHERE deleted_at IS NULL;

COMMIT;
