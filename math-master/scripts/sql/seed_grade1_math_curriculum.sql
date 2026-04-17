-- =============================================================
-- Seed Grade 1 Math curriculum (Vietnamese textbook structure)
-- Hierarchy seeded:
--   SchoolGrade (Lop 1)
--     -> Subject (Toan hoc)
--       -> Chapter (Chu de)
--         -> Lesson (Bai hoc)
--
-- Idempotent by fixed UUID + ON CONFLICT(id).
-- =============================================================

BEGIN;

DO $$
DECLARE
  v_now TIMESTAMPTZ := NOW();

  v_grade_1_id UUID := '91000000-0000-0000-0000-000000000001';
  v_subject_math_g1_id UUID := '91000000-0000-0000-0000-000000000101';
  v_grade_subject_math_g1_id UUID := '91000000-0000-0000-0000-000000000151';

  v_ch1_id UUID := '91000000-0000-0000-0000-000000000201';
  v_ch2_id UUID := '91000000-0000-0000-0000-000000000202';
  v_ch3_id UUID := '91000000-0000-0000-0000-000000000203';
  v_ch4_id UUID := '91000000-0000-0000-0000-000000000204';
  v_ch5_id UUID := '91000000-0000-0000-0000-000000000205';
BEGIN
  -- 1) School grade: Lớp 1
  INSERT INTO school_grades (
    id, grade_level, name, description, is_active,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES (
    v_grade_1_id, 1, 'Lớp 1',
    'Chương trình Toán dành cho học sinh lớp 1.',
    TRUE,
    v_now, NULL, v_now, NULL, NULL, NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    grade_level = EXCLUDED.grade_level,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- 2) Subject: Toán học (for Lớp 1)
  INSERT INTO subjects (
    id, name, code, description, grade_min, grade_max,
    is_active, school_grade_id,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES (
    v_subject_math_g1_id,
    'Toán học',
    'TOAN_HOC_LOP_1',
    'Môn Toán học dành cho học sinh lớp 1.',
    1,
    1,
    TRUE,
    v_grade_1_id,
    v_now, NULL, v_now, NULL, NULL, NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    grade_min = EXCLUDED.grade_min,
    grade_max = EXCLUDED.grade_max,
    is_active = EXCLUDED.is_active,
    school_grade_id = EXCLUDED.school_grade_id,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- Optional N-N mapping table (grade_subjects)
  INSERT INTO grade_subjects (
    id, grade_level, subject_id, is_active,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES (
    v_grade_subject_math_g1_id,
    1,
    v_subject_math_g1_id,
    TRUE,
    v_now, NULL, v_now, NULL, NULL, NULL
  )
  ON CONFLICT (id) DO UPDATE
  SET
    grade_level = EXCLUDED.grade_level,
    subject_id = EXCLUDED.subject_id,
    is_active = EXCLUDED.is_active,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- 3) Chapters (Chu de)
  INSERT INTO chapters (
    id, curriculum_id, subject_id, title, description, order_index,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    (
      v_ch1_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 1: Các số từ 0 đến 10',
      'Làm quen các số trong phạm vi 10, so sánh và luyện tập cơ bản.',
      1,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch2_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 2: Làm quen với một số hình phẳng',
      'Nhận biết hình phẳng cơ bản và thực hành lắp ghép, xếp hình.',
      2,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch3_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 3: Phép cộng, phép trừ trong phạm vi 10',
      'Học phép cộng trừ cơ bản và các bảng cộng trừ trong phạm vi 10.',
      3,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch4_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 4: Làm quen với một số hình khối',
      'Nhận biết hình khối và định hướng trong không gian.',
      4,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch5_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 5: Ôn tập học kì',
      'Ôn tập tổng hợp số học và hình học trong chương trình đã học.',
      5,
      v_now, NULL, v_now, NULL, NULL, NULL
    )
  ON CONFLICT (id) DO UPDATE
  SET
    curriculum_id = EXCLUDED.curriculum_id,
    subject_id = EXCLUDED.subject_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    order_index = EXCLUDED.order_index,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

  -- 4) Lessons (Bai hoc)
  INSERT INTO lessons (
    id, chapter_id, title, learning_objectives, lesson_content, summary,
    order_index, duration_minutes, difficulty, status,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    ('91000000-0000-0000-0000-000000000301', v_ch1_id, 'Tiết học đầu tiên',
      'Làm quen lớp học và cách học môn Toán.',
      'Giới thiệu tiết học đầu tiên, nề nếp học tập và cách sử dụng đồ dùng học toán.',
      'Khởi động năm học với tiết Toán đầu tiên.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 1. Các số 0, 1, 2, 3, 4, 5',
      'Nhận biết, đọc, viết các số từ 0 đến 5.',
      'Học sinh nhận biết ký hiệu, số lượng và thứ tự các số 0-5 qua ví dụ trực quan.',
      'Làm quen dãy số 0 đến 5.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 2. Các số 6, 7, 8, 9, 10',
      'Nhận biết, đọc, viết các số từ 6 đến 10.',
      'Học sinh nhận biết ký hiệu, số lượng và thứ tự các số 6-10 qua hoạt động đếm.',
      'Mở rộng dãy số đến 10.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 3. Nhiều hơn, ít hơn, bằng nhau',
      'So sánh số lượng theo ba quan hệ cơ bản.',
      'Rèn kỹ năng quan sát và so sánh nhiều hơn, ít hơn, bằng nhau bằng đồ vật và tranh ảnh.',
      'Nâng cao kỹ năng so sánh số lượng.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 4. So sánh số',
      'So sánh hai số trong phạm vi 10.',
      'Học sinh sử dụng dấu lớn hơn, bé hơn, bằng nhau để so sánh các cặp số cơ bản.',
      'Biết cách so sánh giá trị hai số.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 5. Mấy và mấy',
      'Phân tích số thành hai phần đơn giản.',
      'Thực hành tách gộp số trong phạm vi 10 qua các bài tập minh họa.',
      'Làm quen cấu trúc tách - gộp số.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000307', v_ch1_id, 'Bài 6. Luyện tập chung',
      'Củng cố kiến thức chủ đề 1.',
      'Tổng hợp bài tập về nhận biết số, so sánh số và tách gộp số trong phạm vi 10.',
      'Ôn luyện tổng hợp chủ đề 1.',
      7, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 7. Hình vuông, hình tròn, hình tam giác, hình chữ nhật',
      'Nhận biết các hình phẳng cơ bản.',
      'Học sinh nhận dạng đặc điểm cơ bản của các hình phẳng thông dụng qua vật thật và tranh vẽ.',
      'Nhận biết 4 hình phẳng cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 8. Thực hành lắp ghép, xếp hình',
      'Vận dụng hình phẳng để lắp ghép hình đơn giản.',
      'Thực hành cắt ghép, xếp hình từ các hình phẳng đã học để phát triển tư duy không gian.',
      'Vận dụng hình phẳng vào hoạt động thực hành.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 9. Luyện tập chung',
      'Củng cố kiến thức hình phẳng.',
      'Tổng hợp bài tập nhận biết và vận dụng các hình phẳng trong tình huống đơn giản.',
      'Ôn luyện chủ đề hình phẳng.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000311', v_ch3_id, 'Bài 10. Phép cộng trong phạm vi 10',
      'Thực hiện phép cộng đơn giản trong phạm vi 10.',
      'Học sinh hiểu ý nghĩa phép cộng và tính toán các phép cộng có tổng không vượt quá 10.',
      'Làm quen phép cộng trong phạm vi 10.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000312', v_ch3_id, 'Bài 11. Phép trừ trong phạm vi 10',
      'Thực hiện phép trừ đơn giản trong phạm vi 10.',
      'Học sinh hiểu ý nghĩa phép trừ và tính toán các phép trừ có hiệu không âm trong phạm vi 10.',
      'Làm quen phép trừ trong phạm vi 10.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000313', v_ch3_id, 'Bài 12. Bảng cộng, bảng trừ trong phạm vi 10',
      'Nhớ và sử dụng bảng cộng trừ cơ bản.',
      'Luyện tập bảng cộng, bảng trừ trong phạm vi 10 để tăng tốc độ và độ chính xác tính toán.',
      'Củng cố bảng cộng bảng trừ.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000314', v_ch3_id, 'Bài 13. Luyện tập chung',
      'Củng cố kiến thức phép cộng trừ trong phạm vi 10.',
      'Tổng hợp bài tập phép cộng, phép trừ và bảng cộng trừ trong phạm vi 10.',
      'Ôn luyện chủ đề phép tính trong phạm vi 10.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000315', v_ch4_id, 'Bài 14. Khối lập phương, khối hộp chữ nhật',
      'Nhận biết hai hình khối cơ bản.',
      'Học sinh nhận dạng đặc điểm ban đầu của khối lập phương và khối hộp chữ nhật qua vật thật.',
      'Làm quen hình khối cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000316', v_ch4_id, 'Bài 15. Vị trí, định hướng trong không gian',
      'Xác định vị trí và hướng cơ bản trong không gian.',
      'Rèn kỹ năng nhận biết trái-phải, trên-dưới, trước-sau trong tình huống thực tế.',
      'Phát triển định hướng không gian cơ bản.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000317', v_ch4_id, 'Bài 16. Luyện tập chung',
      'Củng cố kiến thức hình khối và định hướng không gian.',
      'Tổng hợp bài tập nhận biết khối và xác định vị trí, hướng trong không gian.',
      'Ôn luyện chủ đề hình khối.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000318', v_ch5_id, 'Bài 17. Ôn tập các số trong phạm vi 10',
      'Hệ thống lại kiến thức về các số từ 0 đến 10.',
      'Ôn tập nhận biết, viết, so sánh và sử dụng các số trong phạm vi 10.',
      'Tổng ôn số học cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000319', v_ch5_id, 'Bài 18. Ôn tập phép cộng, phép trừ trong phạm vi 10',
      'Hệ thống lại phép cộng trừ trong phạm vi 10.',
      'Ôn tập các dạng bài phép cộng trừ và bảng cộng trừ đã học.',
      'Tổng ôn phép tính cơ bản.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000320', v_ch5_id, 'Bài 19. Ôn tập hình học',
      'Hệ thống lại kiến thức hình phẳng và hình khối.',
      'Ôn tập nhận biết các hình đã học và vận dụng vào tình huống đơn giản.',
      'Tổng ôn hình học cơ bản.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000321', v_ch5_id, 'Bài 20. Ôn tập chung',
      'Tổng kết và củng cố toàn bộ nội dung học kì.',
      'Tổng hợp bài tập liên kết số học và hình học, chuẩn bị cho giai đoạn học tiếp theo.',
      'Tổng ôn học kì môn Toán lớp 1.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL)
  ON CONFLICT (id) DO UPDATE
  SET
    chapter_id = EXCLUDED.chapter_id,
    title = EXCLUDED.title,
    learning_objectives = EXCLUDED.learning_objectives,
    lesson_content = EXCLUDED.lesson_content,
    summary = EXCLUDED.summary,
    order_index = EXCLUDED.order_index,
    duration_minutes = EXCLUDED.duration_minutes,
    difficulty = EXCLUDED.difficulty,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at,
    updated_by = EXCLUDED.updated_by,
    deleted_at = NULL,
    deleted_by = NULL;

END $$;

COMMIT;
