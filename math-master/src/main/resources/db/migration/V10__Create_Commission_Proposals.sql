-- V10__Create_Commission_Proposals.sql
-- Creates the commission_proposals table for the dynamic revenue-split feature.
-- Teachers submit proposals; admins approve/reject.
-- OrderServiceImpl and EnrollmentServiceImpl read the most-recent APPROVED row
-- to determine the teacher's active revenue share.

CREATE TABLE IF NOT EXISTS commission_proposals (
    id               UUID         NOT NULL,
    teacher_id       UUID         NOT NULL,
    teacher_share    DECIMAL(5,4) NOT NULL,
    platform_share   DECIMAL(5,4) NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    admin_note       TEXT,
    reviewed_by      UUID,
    reviewed_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    created_by       UUID,
    updated_at       TIMESTAMPTZ,
    updated_by       UUID,
    deleted_at       TIMESTAMPTZ,
    deleted_by       UUID,
    CONSTRAINT pk_commission_proposals PRIMARY KEY (id),
    CONSTRAINT fk_commission_proposals_teacher FOREIGN KEY (teacher_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_teacher_share_range CHECK (teacher_share BETWEEN 0.5000 AND 0.9700),
    CONSTRAINT chk_shares_sum          CHECK (ABS((teacher_share + platform_share) - 1.0000) < 0.0001)
);

CREATE INDEX IF NOT EXISTS idx_commission_proposals_teacher_id ON commission_proposals (teacher_id);
CREATE INDEX IF NOT EXISTS idx_commission_proposals_status     ON commission_proposals (status);
