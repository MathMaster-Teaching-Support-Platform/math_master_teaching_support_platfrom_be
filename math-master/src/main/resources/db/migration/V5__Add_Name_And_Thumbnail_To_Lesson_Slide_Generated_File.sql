ALTER TABLE lesson_slide_generated_files
    ADD COLUMN IF NOT EXISTS slide_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS thumbnail_url TEXT;

UPDATE lesson_slide_generated_files
SET slide_name = file_name
WHERE slide_name IS NULL;
