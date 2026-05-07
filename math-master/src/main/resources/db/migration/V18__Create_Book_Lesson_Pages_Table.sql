-- V18: Mapping of (book, lesson) -> page range within the source PDF.
-- One row per Lesson per Book. Validated at the service layer:
--   - page_start/page_end must fall inside book.ocr_page_from..ocr_page_to
--   - When ordered by order_index, lessons[i].page_end <= lessons[i+1].page_start
--     (overlap allowed: a single page may contain content for two consecutive lessons)
--   - All lesson_ids must belong to the book's curriculum

CREATE TABLE IF NOT EXISTS book_lesson_pages (
    id UUID NOT NULL,
    book_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    page_start INTEGER NOT NULL,
    page_end INTEGER NOT NULL,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT pk_book_lesson_pages PRIMARY KEY (id),
    CONSTRAINT fk_blp_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_blp_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    CONSTRAINT uq_blp_book_lesson UNIQUE (book_id, lesson_id),
    CONSTRAINT chk_blp_page_range CHECK (page_start <= page_end),
    CONSTRAINT chk_blp_page_positive CHECK (page_start >= 1)
);

CREATE INDEX IF NOT EXISTS idx_blp_book_order ON book_lesson_pages(book_id, order_index);
CREATE INDEX IF NOT EXISTS idx_blp_lesson ON book_lesson_pages(lesson_id);
