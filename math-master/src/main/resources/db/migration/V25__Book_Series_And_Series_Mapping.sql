-- V25: introduce book_series + series-level lesson mapping without losing old rows.

CREATE TABLE IF NOT EXISTS book_series (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    school_grade_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    academic_year VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT pk_book_series PRIMARY KEY (id),
    CONSTRAINT fk_book_series_school_grade FOREIGN KEY (school_grade_id) REFERENCES school_grades(id),
    CONSTRAINT fk_book_series_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE INDEX IF NOT EXISTS idx_book_series_school_grade ON book_series(school_grade_id);
CREATE INDEX IF NOT EXISTS idx_book_series_subject ON book_series(subject_id);

ALTER TABLE books
    ADD COLUMN IF NOT EXISTS book_series_id UUID;

ALTER TABLE books
    ADD CONSTRAINT fk_books_series
        FOREIGN KEY (book_series_id) REFERENCES book_series(id);

CREATE INDEX IF NOT EXISTS idx_books_series ON books(book_series_id);

-- Backfill safely: each existing book gets its own series to avoid accidental merges.
INSERT INTO book_series (
    id, name, school_grade_id, subject_id, academic_year,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    b.id,
    COALESCE(NULLIF(b.title, ''), 'Book series'),
    b.school_grade_id,
    b.subject_id,
    b.academic_year,
    b.created_at,
    b.created_by,
    b.updated_at,
    b.updated_by,
    b.deleted_at,
    b.deleted_by
FROM books b
WHERE b.book_series_id IS NULL
ON CONFLICT (id) DO NOTHING;

UPDATE books b
SET book_series_id = b.id
WHERE b.book_series_id IS NULL;

CREATE TABLE IF NOT EXISTS book_series_lesson_pages (
    id UUID NOT NULL,
    book_series_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    book_id UUID NOT NULL,
    page_start INTEGER NOT NULL,
    page_end INTEGER NOT NULL,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT pk_book_series_lesson_pages PRIMARY KEY (id),
    CONSTRAINT fk_bslp_series FOREIGN KEY (book_series_id) REFERENCES book_series(id) ON DELETE CASCADE,
    CONSTRAINT fk_bslp_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    CONSTRAINT fk_bslp_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT uq_bslp_series_lesson UNIQUE (book_series_id, lesson_id),
    CONSTRAINT chk_bslp_page_range CHECK (page_start <= page_end),
    CONSTRAINT chk_bslp_page_positive CHECK (page_start >= 1)
);

CREATE INDEX IF NOT EXISTS idx_bslp_series_order ON book_series_lesson_pages(book_series_id, order_index);
CREATE INDEX IF NOT EXISTS idx_bslp_book ON book_series_lesson_pages(book_id);
CREATE INDEX IF NOT EXISTS idx_bslp_lesson ON book_series_lesson_pages(lesson_id);

-- Migrate old per-book mapping into series-level table.
INSERT INTO book_series_lesson_pages (
    id, book_series_id, lesson_id, book_id, page_start, page_end, order_index,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    blp.id,
    b.book_series_id,
    blp.lesson_id,
    blp.book_id,
    blp.page_start,
    blp.page_end,
    blp.order_index,
    blp.created_at,
    blp.created_by,
    blp.updated_at,
    blp.updated_by,
    blp.deleted_at,
    blp.deleted_by
FROM book_lesson_pages blp
JOIN books b ON b.id = blp.book_id
ON CONFLICT (book_series_id, lesson_id) DO NOTHING;

