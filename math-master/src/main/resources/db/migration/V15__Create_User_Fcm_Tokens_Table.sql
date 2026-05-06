CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    device_info VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_user_fcm_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_fcm_tokens_token ON user_fcm_tokens(token);
CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user_active ON user_fcm_tokens(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_last_seen ON user_fcm_tokens(last_seen_at);
