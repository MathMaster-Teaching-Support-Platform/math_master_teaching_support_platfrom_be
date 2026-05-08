CREATE TABLE IF NOT EXISTS feedbacks (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    related_url VARCHAR(500),
    category VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    responded_by UUID REFERENCES users(id),
    response_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_sender ON feedbacks(sender_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON feedbacks(status);
CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON feedbacks(created_at DESC);
