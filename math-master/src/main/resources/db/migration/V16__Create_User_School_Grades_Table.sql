-- Create junction table for Many-to-Many relationship between users and school_grades
CREATE TABLE IF NOT EXISTS user_school_grades (
    user_id UUID NOT NULL,
    school_grade_id UUID NOT NULL,
    PRIMARY KEY (user_id, school_grade_id),
    CONSTRAINT fk_user_school_grades_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_school_grades_grade FOREIGN KEY (school_grade_id) REFERENCES school_grades(id) ON DELETE CASCADE
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_school_grades_user ON user_school_grades(user_id);
CREATE INDEX IF NOT EXISTS idx_user_school_grades_grade ON user_school_grades(school_grade_id);
