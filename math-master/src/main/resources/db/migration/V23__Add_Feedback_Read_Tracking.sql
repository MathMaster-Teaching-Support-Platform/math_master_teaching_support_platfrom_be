ALTER TABLE feedbacks
    ADD COLUMN IF NOT EXISTS sender_read_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS admin_read_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_feedbacks_sender_read_at ON feedbacks(sender_read_at);
CREATE INDEX IF NOT EXISTS idx_feedbacks_admin_read_at ON feedbacks(admin_read_at);
