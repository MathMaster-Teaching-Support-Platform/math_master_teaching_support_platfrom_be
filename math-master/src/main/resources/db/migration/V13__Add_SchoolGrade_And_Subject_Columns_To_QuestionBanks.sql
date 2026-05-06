-- Add missing columns to question_banks table
ALTER TABLE question_banks
    ADD COLUMN IF NOT EXISTS school_grade_id UUID,
    ADD COLUMN IF NOT EXISTS subject_id UUID;

-- Add foreign key constraints if needed
ALTER TABLE question_banks
    ADD CONSTRAINT fk_question_banks_school_grade 
    FOREIGN KEY (school_grade_id) REFERENCES school_grades(id) ON DELETE SET NULL;

ALTER TABLE question_banks
    ADD CONSTRAINT fk_question_banks_subject 
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_question_banks_school_grade ON question_banks(school_grade_id);
CREATE INDEX IF NOT EXISTS idx_question_banks_subject ON question_banks(subject_id);
