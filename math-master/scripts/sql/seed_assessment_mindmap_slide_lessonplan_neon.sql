-- =============================================================
-- Seed script for Neon PostgreSQL
-- Creates/updates:
--   1) assessment (with questions)
--   2) mindmap
--   3) slide template
--   4) lesson plan
--   5) teaching_resources (with MinIO links for slide/mindmap)
--
-- IMPORTANT:
--   1) Replace v_teacher_id and v_lesson_id before running.
--   2) This script is idempotent by fixed IDs + ON CONFLICT (id).
-- =============================================================

BEGIN;

DO $$
DECLARE
  -- TODO: replace these two IDs with real values in your DB.
  v_teacher_id UUID := '11111111-1111-1111-1111-111111111111';
  v_lesson_id  UUID := '22222222-2222-2222-2222-222222222222';

  v_now TIMESTAMPTZ := NOW();

  -- Fixed IDs so script can be rerun safely.
  v_assessment_id UUID := '90000000-0000-0000-0000-000000000001';
  v_question_1_id UUID := '90000000-0000-0000-0000-000000000011';
  v_question_2_id UUID := '90000000-0000-0000-0000-000000000012';
  v_mindmap_id UUID := '90000000-0000-0000-0000-000000000021';
  v_slide_template_id UUID := '90000000-0000-0000-0000-000000000031';
  v_lesson_plan_id UUID := '90000000-0000-0000-0000-000000000041';
  v_resource_slide_id UUID := '90000000-0000-0000-0000-000000000051';
  v_resource_mindmap_id UUID := '90000000-0000-0000-0000-000000000052';
BEGIN
  -- Validate referenced teacher and lesson.
  IF NOT EXISTS (SELECT 1 FROM users u WHERE u.id = v_teacher_id AND u.deleted_at IS NULL) THEN
    RAISE EXCEPTION 'Teacher ID % not found in users table.', v_teacher_id;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM lessons l WHERE l.id = v_lesson_id AND l.deleted_at IS NULL) THEN
    RAISE EXCEPTION 'Lesson ID % not found in lessons table.', v_lesson_id;
  END IF;

  -- -------------------------------------------------------------
  -- 1) Questions (for assessment)
  -- -------------------------------------------------------------
  INSERT INTO questions (
    id,
    question_bank_id,
    question_type,
    question_text,
    options,
    correct_answer,
    explanation,
    points,
    difficulty,
    cognitive_level,
    question_status,
    question_source_type,
    bloom_taxonomy_tags,
    learning_objectives,
    tags,
    template_id,
    generation_metadata,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES
  (
    v_question_1_id,
    NULL,
    'MULTIPLE_CHOICE',
    'What is the derivative of x^2?',
    '{"A":"x", "B":"2x", "C":"x^2", "D":"2"}'::jsonb,
    'B',
    'The derivative of x^n is n*x^(n-1).',
    1.00,
    'EASY',
    'UNDERSTAND',
    'APPROVED',
    'MANUAL',
    ARRAY['UNDERSTAND'],
    ARRAY['Basic derivative rules'],
    ARRAY['calculus','derivative'],
    NULL,
    '{"seed":"neon-script"}'::jsonb,
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  ),
  (
    v_question_2_id,
    NULL,
    'MULTIPLE_CHOICE',
    'Integral of 2x dx equals?',
    '{"A":"x^2 + C", "B":"2x + C", "C":"x + C", "D":"x^2/2 + C"}'::jsonb,
    'A',
    'The integral of 2x is x^2 + C.',
    1.00,
    'EASY',
    'APPLY',
    'APPROVED',
    'MANUAL',
    ARRAY['APPLY'],
    ARRAY['Basic integral rules'],
    ARRAY['calculus','integral'],
    NULL,
    '{"seed":"neon-script"}'::jsonb,
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    question_text = EXCLUDED.question_text,
    options = EXCLUDED.options,
    correct_answer = EXCLUDED.correct_answer,
    explanation = EXCLUDED.explanation,
    points = EXCLUDED.points,
    difficulty = EXCLUDED.difficulty,
    cognitive_level = EXCLUDED.cognitive_level,
    question_status = EXCLUDED.question_status,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- -------------------------------------------------------------
  -- 2) Assessment
  -- -------------------------------------------------------------
  INSERT INTO assessments (
    id,
    teacher_id,
    title,
    description,
    assessment_type,
    time_limit_minutes,
    passing_score,
    start_date,
    end_date,
    randomize_questions,
    show_correct_answers,
    assessment_mode,
    exam_matrix_id,
    allow_multiple_attempts,
    max_attempts,
    attempt_scoring_policy,
    show_score_immediately,
    status,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES (
    v_assessment_id,
    v_teacher_id,
    'Seed Assessment - Calculus Basics',
    'Auto-seeded assessment for Neon DB setup.',
    'QUIZ',
    20,
    70.00,
    NULL,
    NULL,
    TRUE,
    TRUE,
    'DIRECT',
    NULL,
    TRUE,
    3,
    'BEST',
    TRUE,
    'DRAFT',
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    assessment_type = EXCLUDED.assessment_type,
    time_limit_minutes = EXCLUDED.time_limit_minutes,
    passing_score = EXCLUDED.passing_score,
    randomize_questions = EXCLUDED.randomize_questions,
    show_correct_answers = EXCLUDED.show_correct_answers,
    assessment_mode = EXCLUDED.assessment_mode,
    allow_multiple_attempts = EXCLUDED.allow_multiple_attempts,
    max_attempts = EXCLUDED.max_attempts,
    attempt_scoring_policy = EXCLUDED.attempt_scoring_policy,
    show_score_immediately = EXCLUDED.show_score_immediately,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- Link assessment with questions.
  INSERT INTO assessment_questions (
    id,
    assessment_id,
    question_id,
    order_index,
    points_override,
    matrix_template_mapping_id,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES
  (
    '90000000-0000-0000-0000-000000000101',
    v_assessment_id,
    v_question_1_id,
    1,
    NULL,
    NULL,
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  ),
  (
    '90000000-0000-0000-0000-000000000102',
    v_assessment_id,
    v_question_2_id,
    2,
    NULL,
    NULL,
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    assessment_id = EXCLUDED.assessment_id,
    question_id = EXCLUDED.question_id,
    order_index = EXCLUDED.order_index,
    points_override = EXCLUDED.points_override,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- -------------------------------------------------------------
  -- 3) Mindmap
  -- -------------------------------------------------------------
  INSERT INTO mindmaps (
    id,
    teacher_id,
    lesson_id,
    title,
    description,
    ai_generated,
    generation_prompt,
    status,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES (
    v_mindmap_id,
    v_teacher_id,
    v_lesson_id,
    'Seed Mindmap - Derivative Rules',
    'Mindmap metadata record seeded for testing.',
    FALSE,
    'Seeded from SQL script',
    'PUBLISHED',
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- -------------------------------------------------------------
  -- 4) Slide template
  -- MinIO link is represented by bucket_name + object_key.
  -- -------------------------------------------------------------
  INSERT INTO slide_templates (
    id,
    name,
    description,
    original_file_name,
    content_type,
    object_key,
    preview_image_object_key,
    preview_image_content_type,
    bucket_name,
    uploaded_by,
    is_active,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES (
    v_slide_template_id,
    'Seed Slide Template - Algebra Deck',
    'PPTX template stored in MinIO.',
    'algebra-template.pptx',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'slide-templates/seed/algebra-template.pptx',
    'slide-templates/seed/algebra-template-preview.png',
    'image/png',
    'slide-templates',
    v_teacher_id,
    TRUE,
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    object_key = EXCLUDED.object_key,
    preview_image_object_key = EXCLUDED.preview_image_object_key,
    preview_image_content_type = EXCLUDED.preview_image_content_type,
    bucket_name = EXCLUDED.bucket_name,
    is_active = EXCLUDED.is_active,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- -------------------------------------------------------------
  -- 5) Lesson plan
  -- -------------------------------------------------------------
  INSERT INTO lesson_plans (
    id,
    lesson_id,
    teacher_id,
    objectives,
    materials_needed,
    teaching_strategy,
    assessment_methods,
    notes,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES (
    v_lesson_plan_id,
    v_lesson_id,
    v_teacher_id,
    ARRAY['Understand derivative definition', 'Apply basic derivative rules'],
    ARRAY['Projector', 'Worksheet', 'Whiteboard'],
    'Guided instruction + worked examples + quick checks.',
    'Exit ticket with 3 questions + in-class Q&A.',
    'Seed lesson plan for integration testing.',
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    objectives = EXCLUDED.objectives,
    materials_needed = EXCLUDED.materials_needed,
    teaching_strategy = EXCLUDED.teaching_strategy,
    assessment_methods = EXCLUDED.assessment_methods,
    notes = EXCLUDED.notes,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- -------------------------------------------------------------
  -- 6) Optional teaching_resources with direct MinIO links.
  -- This is useful when FE needs explicit downloadable URL records.
  -- -------------------------------------------------------------
  INSERT INTO teaching_resources (
    id,
    name,
    type,
    file_url,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
  )
  VALUES
  (
    v_resource_slide_id,
    'Seed Slide File (MinIO URL)',
    'SLIDE',
    'https://<your-minio-domain>/slide-templates/seed/algebra-template.pptx',
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  ),
  (
    v_resource_mindmap_id,
    'Seed Mindmap File (MinIO URL)',
    'MINDMAP',
    'https://<your-minio-domain>/mindmaps/seed/derivative-rules.mm',
    v_now,
    v_teacher_id,
    v_now,
    v_teacher_id,
    NULL,
    NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    file_url = EXCLUDED.file_url,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

END $$;

COMMIT;
