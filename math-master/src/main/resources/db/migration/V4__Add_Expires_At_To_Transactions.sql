-- V4: Add expires_at column to transactions for PENDING auto-cancel after 15 minutes
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
