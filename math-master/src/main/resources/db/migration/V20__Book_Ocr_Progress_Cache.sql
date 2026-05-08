-- Last-known OCR job snapshot from Python/Mongo, persisted when Java successfully polls
-- /ocr-status. Lets the admin UI show progress after closing the tab or if the crawler is briefly unreachable.

ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_runner_status VARCHAR(32);
ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_phase VARCHAR(80);
ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_progress_percent INTEGER;
ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_processed_pages INTEGER;
ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_total_pages INTEGER;
ALTER TABLE books ADD COLUMN IF NOT EXISTS ocr_cached_at TIMESTAMP;
