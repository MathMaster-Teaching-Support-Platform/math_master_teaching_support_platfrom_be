-- Curriculum is no longer a required field for textbooks.
-- The admin book wizard navigates Grade > Subject > Chapter > Lesson;
-- a book may optionally be attached to a curriculum later.
ALTER TABLE books
    ALTER COLUMN curriculum_id DROP NOT NULL;
