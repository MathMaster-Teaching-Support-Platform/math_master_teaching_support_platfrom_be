-- Add token quota fields for subscription plans and user subscriptions

ALTER TABLE subscription_plans
  ADD COLUMN IF NOT EXISTS token_quota INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_subscriptions
  ADD COLUMN IF NOT EXISTS token_quota INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_subscriptions
  ADD COLUMN IF NOT EXISTS token_remaining INTEGER NOT NULL DEFAULT 0;
