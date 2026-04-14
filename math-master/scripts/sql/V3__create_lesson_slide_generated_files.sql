-- =============================================================
-- Migration: create lesson_slide_generated_files
-- Purpose: store generated PPTX metadata for re-download and public sharing
-- Compatible with: PostgreSQL (Neon or local)
-- =============================================================

BEGIN;

CREATE TABLE IF NOT EXISTS lesson_slide_generated_files (
    id              UUID         NOT NULL PRIMARY KEY,
    lesson_id       UUID         NOT NULL REFERENCES lessons (id),
    template_id     UUID,
    bucket_name     VARCHAR(255) NOT NULL,
    object_key      TEXT         NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT       NOT NULL,
    is_public       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);

CREATE INDEX IF NOT EXISTS idx_slide_generated_lesson
    ON lesson_slide_generated_files (lesson_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_slide_generated_created_by
    ON lesson_slide_generated_files (created_by)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_slide_generated_public
    ON lesson_slide_generated_files (is_public)
    WHERE deleted_at IS NULL;

COMMIT;
