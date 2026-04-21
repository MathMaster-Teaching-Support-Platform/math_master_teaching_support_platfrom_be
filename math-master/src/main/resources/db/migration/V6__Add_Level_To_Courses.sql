-- V6: Add level column to courses table for difficulty/experience level
ALTER TABLE courses ADD COLUMN IF NOT EXISTS level VARCHAR(20) DEFAULT 'ALL_LEVELS';
