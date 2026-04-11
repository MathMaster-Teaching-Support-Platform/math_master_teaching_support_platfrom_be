-- Add OCR-related fields to teacher_profiles table

-- Add personal information fields for OCR comparison
ALTER TABLE teacher_profiles
ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS id_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS date_of_birth TIMESTAMP,
ADD COLUMN IF NOT EXISTS place_of_birth VARCHAR(255),
ADD COLUMN IF NOT EXISTS address TEXT;

-- Add verification document path (MinIO path)
ALTER TABLE teacher_profiles
ADD COLUMN IF NOT EXISTS verification_document_path VARCHAR(500);

-- Add OCR verification result fields
ALTER TABLE teacher_profiles
ADD COLUMN IF NOT EXISTS ocr_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS ocr_match_score DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS ocr_verification_data TEXT,
ADD COLUMN IF NOT EXISTS ocr_verified_at TIMESTAMP;

-- Add comments
COMMENT ON COLUMN teacher_profiles.full_name IS 'Full name from profile for OCR comparison';
COMMENT ON COLUMN teacher_profiles.id_number IS 'ID card number for OCR comparison';
COMMENT ON COLUMN teacher_profiles.date_of_birth IS 'Date of birth for OCR comparison';
COMMENT ON COLUMN teacher_profiles.place_of_birth IS 'Place of birth for OCR comparison';
COMMENT ON COLUMN teacher_profiles.address IS 'Address for OCR comparison';
COMMENT ON COLUMN teacher_profiles.verification_document_path IS 'MinIO path to verification document';
COMMENT ON COLUMN teacher_profiles.ocr_verified IS 'Whether OCR verification was performed';
COMMENT ON COLUMN teacher_profiles.ocr_match_score IS 'OCR match score percentage (0-100)';
COMMENT ON COLUMN teacher_profiles.ocr_verification_data IS 'JSON data from OCR verification';
COMMENT ON COLUMN teacher_profiles.ocr_verified_at IS 'Timestamp when OCR verification was performed';
