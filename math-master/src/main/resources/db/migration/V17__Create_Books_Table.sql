-- V17: Create books table for OCR textbook ingestion.
-- A Book is the source PDF tied to a specific Grade + Subject + Curriculum
-- (e.g., "Toán 10 - Kết nối tri thức"). OCR content per page lives in MongoDB,
-- but the canonical hierarchy and verify-state lives here in Postgres.

CREATE TABLE IF NOT EXISTS books (
    id UUID NOT NULL,
    school_grade_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    curriculum_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    academic_year VARCHAR(50),
    pdf_path VARCHAR(500),
    thumbnail_path VARCHAR(500),
    total_pages INTEGER,
    ocr_page_from INTEGER,
    ocr_page_to INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    ocr_error TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT fk_books_school_grade FOREIGN KEY (school_grade_id) REFERENCES school_grades(id),
    CONSTRAINT fk_books_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_books_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula(id),
    CONSTRAINT chk_books_ocr_window CHECK (
        ocr_page_from IS NULL OR ocr_page_to IS NULL OR ocr_page_from <= ocr_page_to
    ),
    CONSTRAINT chk_books_total_pages CHECK (total_pages IS NULL OR total_pages > 0)
);

CREATE INDEX IF NOT EXISTS idx_books_school_grade ON books(school_grade_id);
CREATE INDEX IF NOT EXISTS idx_books_subject ON books(subject_id);
CREATE INDEX IF NOT EXISTS idx_books_curriculum ON books(curriculum_id);
CREATE INDEX IF NOT EXISTS idx_books_status ON books(status);
CREATE INDEX IF NOT EXISTS idx_books_verified ON books(verified);
