-- Move roadmap topic course linkage from single course_id to many-to-many table.
-- Safe to run on environments where some old columns/tables may not exist.

CREATE TABLE IF NOT EXISTS roadmap_topic_courses (
    roadmap_topic_id UUID NOT NULL,
    course_id UUID NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (roadmap_topic_id, course_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_roadmap_topic_courses_topic'
    ) THEN
        ALTER TABLE roadmap_topic_courses
            ADD CONSTRAINT fk_roadmap_topic_courses_topic
            FOREIGN KEY (roadmap_topic_id) REFERENCES roadmap_topics(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_roadmap_topic_courses_course'
    ) THEN
        ALTER TABLE roadmap_topic_courses
            ADD CONSTRAINT fk_roadmap_topic_courses_course
            FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_roadmap_topic_courses_topic_id
    ON roadmap_topic_courses (roadmap_topic_id);

CREATE INDEX IF NOT EXISTS idx_roadmap_topic_courses_course_id
    ON roadmap_topic_courses (course_id);

-- Backfill legacy one-to-one data if old column still exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'roadmap_topics'
          AND column_name = 'course_id'
    ) THEN
        INSERT INTO roadmap_topic_courses (roadmap_topic_id, course_id)
        SELECT rt.id, rt.course_id
        FROM roadmap_topics rt
        WHERE rt.course_id IS NOT NULL
        ON CONFLICT DO NOTHING;

        ALTER TABLE roadmap_topics DROP COLUMN IF EXISTS course_id;
    END IF;
END $$;
