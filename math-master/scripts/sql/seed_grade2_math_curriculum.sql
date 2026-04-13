-- =============================================================
-- Seed Grade 2 Math curriculum (Vietnamese textbook structure)
-- Hierarchy seeded:
--   SchoolGrade (Lớp 2)
--     -> Subject (Toán học)
--       -> Chapter (Chủ đề 1 -> 14)
--         -> Lesson (Bài 1 -> 75)
--
-- Idempotent by fixed UUID + ON CONFLICT(id).
-- =============================================================

BEGIN;

DO $$
DECLARE
  v_now TIMESTAMPTZ := NOW();

  v_grade_2_id UUID := '92000000-0000-0000-0000-000000000001';
  v_subject_math_g2_id UUID := '92000000-0000-0000-0000-000000000101';
  v_grade_subject_math_g2_id UUID := '92000000-0000-0000-0000-000000000151';

  v_ch1_id UUID := '92000000-0000-0000-0000-000000000201';
  v_ch2_id UUID := '92000000-0000-0000-0000-000000000202';
  v_ch3_id UUID := '92000000-0000-0000-0000-000000000203';
  v_ch4_id UUID := '92000000-0000-0000-0000-000000000204';
  v_ch5_id UUID := '92000000-0000-0000-0000-000000000205';
  v_ch6_id UUID := '92000000-0000-0000-0000-000000000206';
  v_ch7_id UUID := '92000000-0000-0000-0000-000000000207';
  v_ch8_id UUID := '92000000-0000-0000-0000-000000000208';
  v_ch9_id UUID := '92000000-0000-0000-0000-000000000209';
  v_ch10_id UUID := '92000000-0000-0000-0000-000000000210';
  v_ch11_id UUID := '92000000-0000-0000-0000-000000000211';
  v_ch12_id UUID := '92000000-0000-0000-0000-000000000212';
  v_ch13_id UUID := '92000000-0000-0000-0000-000000000213';
  v_ch14_id UUID := '92000000-0000-0000-0000-000000000214';
BEGIN
  -- Reuse existing IDs when unique keys already exist with different UUIDs.
  SELECT id INTO v_grade_2_id
  FROM school_grades
  WHERE grade_level = 2
  LIMIT 1;

  IF v_grade_2_id IS NULL THEN
    v_grade_2_id := '92000000-0000-0000-0000-000000000001';
  END IF;

  SELECT id INTO v_subject_math_g2_id
  FROM subjects
  WHERE code = 'TOAN_HOC_LOP_2'
  LIMIT 1;

  IF v_subject_math_g2_id IS NULL THEN
    v_subject_math_g2_id := '92000000-0000-0000-0000-000000000101';
  END IF;

  SELECT id INTO v_grade_subject_math_g2_id
  FROM grade_subjects
  WHERE grade_level = 2
    AND subject_id = v_subject_math_g2_id
  LIMIT 1;

  IF v_grade_subject_math_g2_id IS NULL THEN
    v_grade_subject_math_g2_id := '92000000-0000-0000-0000-000000000151';
  END IF;

  -- 1) School grade: Lớp 2
  INSERT INTO school_grades (
    id, grade_level, name, description, is_active,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES (
    v_grade_2_id, 2, 'Lớp 2',
    'Chương trình Toán dành cho học sinh lớp 2.',
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

  -- 2) Subject: Toán học (for Lớp 2)
  INSERT INTO subjects (
    id, name, code, description, grade_min, grade_max,
    is_active, school_grade_id,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES (
    v_subject_math_g2_id,
    'Toán học',
    'TOAN_HOC_LOP_2',
    'Môn Toán học dành cho học sinh lớp 2.',
    2,
    2,
    TRUE,
    v_grade_2_id,
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
    v_grade_subject_math_g2_id,
    2,
    v_subject_math_g2_id,
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

  -- 3) Chapters (Chủ đề)
  INSERT INTO chapters (
    id, curriculum_id, subject_id, title, description, order_index,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    (v_ch1_id, NULL, v_subject_math_g2_id, 'Chủ đề 1: Ôn tập và bổ sung',
      'Ôn tập kiến thức nền tảng và củng cố các phép tính trong phạm vi 100.',
      1, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch2_id, NULL, v_subject_math_g2_id, 'Chủ đề 2: Phép cộng, phép trừ trong phạm vi 20',
      'Luyện tập phép cộng, phép trừ qua 10 và giải toán có lời văn đơn giản.',
      2, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch3_id, NULL, v_subject_math_g2_id, 'Chủ đề 3: Làm quen với khối lượng, dung tích',
      'Làm quen đơn vị ki-lô-gam, lít và các hoạt động thực hành đo lường.',
      3, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch4_id, NULL, v_subject_math_g2_id, 'Chủ đề 4: Phép cộng, phép trừ (có nhớ) trong phạm vi 100',
      'Thực hiện phép tính có nhớ với số có hai chữ số trong phạm vi 100.',
      4, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch5_id, NULL, v_subject_math_g2_id, 'Chủ đề 5: Làm quen với hình phẳng',
      'Nhận biết các yếu tố hình học phẳng cơ bản và thao tác thực hành.',
      5, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch6_id, NULL, v_subject_math_g2_id, 'Chủ đề 6: Ngày - giờ, giờ - phút, ngày - tháng',
      'Làm quen thời gian, xem đồng hồ và đọc lịch trong sinh hoạt hằng ngày.',
      6, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch7_id, NULL, v_subject_math_g2_id, 'Chủ đề 7: Ôn tập học kì I',
      'Tổng hợp kiến thức học kì I về số học, hình học và đo lường.',
      7, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch8_id, NULL, v_subject_math_g2_id, 'Chủ đề 8: Phép nhân, phép chia',
      'Làm quen phép nhân, phép chia và các bảng nhân chia cơ bản.',
      8, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch9_id, NULL, v_subject_math_g2_id, 'Chủ đề 9: Làm quen với hình khối',
      'Nhận biết khối trụ, khối cầu và vận dụng trong thực tế.',
      9, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch10_id, NULL, v_subject_math_g2_id, 'Chủ đề 10: Các số trong phạm vi 1 000',
      'Mở rộng số học đến phạm vi 1 000 và so sánh số có ba chữ số.',
      10, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch11_id, NULL, v_subject_math_g2_id, 'Chủ đề 11: Độ dài và đơn vị đo độ dài. Tiền Việt Nam',
      'Làm quen đơn vị đo độ dài và nhận biết tiền Việt Nam.',
      11, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch12_id, NULL, v_subject_math_g2_id, 'Chủ đề 12: Phép cộng, phép trừ trong phạm vi 1 000',
      'Thực hiện cộng trừ có nhớ và không nhớ trong phạm vi 1 000.',
      12, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch13_id, NULL, v_subject_math_g2_id, 'Chủ đề 13: Làm quen với yếu tố thống kê, xác suất',
      'Thu thập, phân loại số liệu và nhận biết khả năng xảy ra của sự kiện.',
      13, v_now, NULL, v_now, NULL, NULL, NULL),

    (v_ch14_id, NULL, v_subject_math_g2_id, 'Chủ đề 14: Ôn tập cuối năm',
      'Tổng ôn toàn bộ kiến thức Toán lớp 2.',
      14, v_now, NULL, v_now, NULL, NULL, NULL)
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

  -- 4) Lessons (Bài học)
  INSERT INTO lessons (
    id, chapter_id, title, learning_objectives, lesson_content, summary,
    order_index, duration_minutes, difficulty, status,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    ('92000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Ôn tập các số đến 100',
      'Ôn tập đọc, viết và so sánh các số trong phạm vi 100.',
      'Học sinh củng cố kiến thức về số đến 100 thông qua bài tập nhận biết và sắp xếp số.',
      'Củng cố kiến thức số học trong phạm vi 100.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Tia số. Số liền trước, số liền sau',
      'Nhận biết tia số và xác định số liền trước, liền sau.',
      'Học sinh luyện tìm vị trí số trên tia số và xác định quan hệ liền kề giữa các số.',
      'Rèn kỹ năng làm việc với tia số.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Các thành phần của phép cộng, phép trừ',
      'Nhận biết tên gọi các thành phần trong phép cộng, phép trừ.',
      'Học sinh gọi đúng tên các số trong phép cộng và phép trừ, vận dụng trong tính toán.',
      'Nắm vững thuật ngữ cơ bản của phép tính.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Hơn, kém nhau bao nhiêu',
      'Giải bài toán tìm phần hơn kém giữa hai số.',
      'Học sinh vận dụng phép trừ để xác định độ chênh lệch giữa hai đại lượng.',
      'Làm quen dạng toán hơn kém.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Ôn tập phép cộng, phép trừ (không nhớ) trong phạm vi 100',
      'Củng cố phép cộng trừ không nhớ trong phạm vi 100.',
      'Học sinh luyện các dạng bài cộng trừ cơ bản để tăng độ chính xác.',
      'Ôn tập phép tính không nhớ phạm vi 100.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 6. Luyện tập chung',
      'Tổng hợp và củng cố kiến thức chủ đề 1.',
      'Học sinh hoàn thành bài tập tổng hợp về số học và phép tính đã ôn.',
      'Ôn luyện chủ đề 1.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000307', v_ch2_id, 'Bài 7. Phép cộng (qua 10) trong phạm vi 20',
      'Thực hiện phép cộng qua 10 trong phạm vi 20.',
      'Học sinh thực hành cộng có nhớ một lần trong phạm vi 20 bằng nhiều cách.',
      'Làm quen cộng qua 10.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 8. Bảng cộng (qua 10)',
      'Ghi nhớ và sử dụng bảng cộng qua 10.',
      'Học sinh luyện tập bảng cộng qua 10 để tính nhanh và chính xác hơn.',
      'Củng cố bảng cộng qua 10.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 9. Bài toán về thêm, bớt một số đơn vị',
      'Giải bài toán có lời văn dạng thêm bớt đơn vị.',
      'Học sinh phân tích đề và chọn phép tính phù hợp cho từng tình huống thêm hoặc bớt.',
      'Vận dụng phép cộng trừ vào bài toán thực tế.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 10. Luyện tập chung',
      'Củng cố kiến thức phép cộng trong phạm vi 20.',
      'Học sinh luyện tổng hợp các dạng bài cộng qua 10 và bài toán có lời văn.',
      'Ôn luyện chủ đề 2 (phần cộng).',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000311', v_ch2_id, 'Bài 11. Phép trừ (qua 10) trong phạm vi 20',
      'Thực hiện phép trừ qua 10 trong phạm vi 20.',
      'Học sinh luyện trừ có nhớ trong phạm vi 20 qua các bài tập trực quan.',
      'Làm quen trừ qua 10.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000312', v_ch2_id, 'Bài 12. Bảng trừ (qua 10)',
      'Ghi nhớ và sử dụng bảng trừ qua 10.',
      'Học sinh luyện tập bảng trừ qua 10 để tăng kỹ năng tính nhẩm.',
      'Củng cố bảng trừ qua 10.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000313', v_ch2_id, 'Bài 13. Bài toán về nhiều hơn, ít hơn một số đơn vị',
      'Giải bài toán nhiều hơn, ít hơn một số đơn vị.',
      'Học sinh phân tích dữ kiện và xác định phép tính để giải toán chênh lệch.',
      'Rèn kỹ năng giải toán có lời văn.',
      7, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000314', v_ch2_id, 'Bài 14. Luyện tập chung',
      'Tổng hợp kiến thức phép cộng, phép trừ trong phạm vi 20.',
      'Học sinh luyện tập tổng hợp các dạng bài đã học trong chủ đề 2.',
      'Ôn luyện chủ đề 2.',
      8, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000315', v_ch3_id, 'Bài 15. Ki-lô-gam',
      'Nhận biết đơn vị đo khối lượng ki-lô-gam.',
      'Học sinh làm quen cân nặng và sử dụng ki-lô-gam trong tình huống thực tế.',
      'Làm quen đơn vị khối lượng.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 16. Lít',
      'Nhận biết đơn vị đo dung tích lít.',
      'Học sinh quan sát vật chứa và bước đầu đo dung tích bằng lít.',
      'Làm quen đơn vị dung tích.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài 17. Thực hành và trải nghiệm với các đơn vị ki-lô-gam, lít',
      'Vận dụng đơn vị ki-lô-gam, lít trong thực hành.',
      'Học sinh thực hiện hoạt động cân, đong và giải quyết tình huống gần gũi.',
      'Rèn kỹ năng đo lường thực tế.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000318', v_ch3_id, 'Bài 18. Luyện tập chung',
      'Củng cố kiến thức khối lượng và dung tích.',
      'Học sinh luyện tổng hợp các bài tập liên quan đến ki-lô-gam và lít.',
      'Ôn luyện chủ đề 3.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000319', v_ch4_id, 'Bài 19. Phép cộng (có nhớ) số có hai chữ số với số có một chữ số',
      'Thực hiện cộng có nhớ dạng hai chữ số cộng một chữ số.',
      'Học sinh rèn kỹ năng đặt tính và tính đúng với phép cộng có nhớ.',
      'Làm quen cộng có nhớ dạng cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000320', v_ch4_id, 'Bài 20. Phép cộng (có nhớ) số có hai chữ số với số có hai chữ số',
      'Thực hiện cộng có nhớ giữa hai số có hai chữ số.',
      'Học sinh luyện cộng có nhớ theo cột dọc và kiểm tra kết quả.',
      'Rèn kỹ năng cộng có nhớ trong phạm vi 100.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000321', v_ch4_id, 'Bài 21. Luyện tập chung',
      'Củng cố phép cộng có nhớ trong phạm vi 100.',
      'Học sinh luyện tập tổng hợp các dạng cộng có nhớ đã học.',
      'Ôn luyện chủ đề 4 (phần cộng).',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000322', v_ch4_id, 'Bài 22. Phép trừ (có nhớ) số có hai chữ số cho số có một chữ số',
      'Thực hiện trừ có nhớ dạng hai chữ số trừ một chữ số.',
      'Học sinh luyện đặt tính và trừ có nhớ chính xác.',
      'Làm quen trừ có nhớ dạng cơ bản.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000323', v_ch4_id, 'Bài 23. Phép trừ (có nhớ) số có hai chữ số cho số có hai chữ số',
      'Thực hiện trừ có nhớ giữa hai số có hai chữ số.',
      'Học sinh rèn kỹ năng trừ có nhớ và kiểm tra kết quả phép tính.',
      'Rèn kỹ năng trừ có nhớ trong phạm vi 100.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000324', v_ch4_id, 'Bài 24. Luyện tập chung',
      'Tổng hợp phép cộng, phép trừ có nhớ trong phạm vi 100.',
      'Học sinh luyện tập tổng hợp các dạng phép tính có nhớ trong chủ đề 4.',
      'Ôn luyện chủ đề 4.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000325', v_ch5_id, 'Bài 25. Điểm, đoạn thẳng, đường thẳng, đường cong, ba điểm thẳng hàng',
      'Nhận biết các yếu tố hình học phẳng cơ bản.',
      'Học sinh phân biệt điểm, đoạn thẳng, đường thẳng, đường cong và ba điểm thẳng hàng.',
      'Làm quen khái niệm hình học phẳng.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000326', v_ch5_id, 'Bài 26. Đường gấp khúc. Hình tứ giác',
      'Nhận biết đường gấp khúc và hình tứ giác.',
      'Học sinh quan sát, vẽ đơn giản và phân loại hình liên quan đến đường gấp khúc, tứ giác.',
      'Củng cố kiến thức hình học phẳng.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000327', v_ch5_id, 'Bài 27. Thực hành gấp, cắt, ghép, xếp hình. Vẽ đoạn thẳng',
      'Vận dụng thao tác thực hành để nhận biết hình phẳng.',
      'Học sinh thực hành gấp, cắt, ghép, xếp hình và vẽ đoạn thẳng theo yêu cầu.',
      'Rèn kỹ năng thực hành hình học.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000328', v_ch5_id, 'Bài 28. Luyện tập chung',
      'Củng cố kiến thức hình phẳng trong chủ đề 5.',
      'Học sinh luyện tổng hợp các bài tập nhận biết và thực hành hình học phẳng.',
      'Ôn luyện chủ đề 5.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000329', v_ch6_id, 'Bài 29. Ngày - giờ, giờ - phút',
      'Nhận biết quan hệ giữa ngày, giờ, phút.',
      'Học sinh thực hành xem đồng hồ và chuyển đổi đơn vị thời gian đơn giản.',
      'Làm quen đơn vị thời gian cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000330', v_ch6_id, 'Bài 30. Ngày - tháng',
      'Nhận biết ngày, tháng trên lịch.',
      'Học sinh đọc lịch, xác định ngày trong tháng và thứ trong tuần.',
      'Rèn kỹ năng xem lịch.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000331', v_ch6_id, 'Bài 31. Thực hành và trải nghiệm xem đồng hồ, xem lịch',
      'Vận dụng xem đồng hồ và xem lịch trong thực tế.',
      'Học sinh xử lý tình huống sinh hoạt hằng ngày liên quan đến thời gian.',
      'Củng cố kỹ năng về thời gian.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000332', v_ch6_id, 'Bài 32. Luyện tập chung',
      'Củng cố kiến thức thời gian và lịch.',
      'Học sinh luyện tổng hợp bài tập về ngày, giờ, phút, tháng.',
      'Ôn luyện chủ đề 6.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000333', v_ch7_id, 'Bài 33. Ôn tập phép cộng, phép trừ trong phạm vi 20, 100',
      'Ôn tập tổng hợp phép cộng, phép trừ trong phạm vi 20 và 100.',
      'Học sinh thực hành bài tập tổng hợp để củng cố kỹ năng tính toán.',
      'Tổng ôn số học học kì I.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000334', v_ch7_id, 'Bài 34. Ôn tập hình phẳng',
      'Ôn tập kiến thức hình học phẳng đã học.',
      'Học sinh nhận biết lại các yếu tố hình học phẳng qua bài tập thực hành.',
      'Tổng ôn hình phẳng học kì I.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000335', v_ch7_id, 'Bài 35. Ôn tập đo lường',
      'Ôn tập các đơn vị đo và cách sử dụng đã học.',
      'Học sinh ôn tập ki-lô-gam, lít, thời gian qua bài tập tổng hợp.',
      'Tổng ôn đo lường học kì I.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000336', v_ch7_id, 'Bài 36. Ôn tập chung',
      'Tổng kết kiến thức học kì I.',
      'Học sinh thực hiện bài tập tổng hợp liên môn trong học kì I.',
      'Hoàn thiện kiến thức học kì I.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000337', v_ch8_id, 'Bài 37. Phép nhân',
      'Làm quen khái niệm phép nhân.',
      'Học sinh hiểu phép nhân là phép cộng các số bằng nhau trong tình huống đơn giản.',
      'Bước đầu tiếp cận phép nhân.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000338', v_ch8_id, 'Bài 38. Thừa số, tích',
      'Nhận biết các thành phần trong phép nhân.',
      'Học sinh gọi tên thừa số, tích và vận dụng trong các phép tính đơn giản.',
      'Nắm thuật ngữ phép nhân.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000339', v_ch8_id, 'Bài 39. Bảng nhân 2',
      'Ghi nhớ và sử dụng bảng nhân 2.',
      'Học sinh luyện bảng nhân 2 qua bài tập tính nhẩm và ứng dụng.',
      'Củng cố bảng nhân 2.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000340', v_ch8_id, 'Bài 40. Bảng nhân 5',
      'Ghi nhớ và sử dụng bảng nhân 5.',
      'Học sinh luyện bảng nhân 5 qua bài tập tính và tình huống thực tế.',
      'Củng cố bảng nhân 5.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000341', v_ch8_id, 'Bài 41. Phép chia',
      'Làm quen khái niệm phép chia.',
      'Học sinh hiểu phép chia là chia đều thành các phần bằng nhau.',
      'Bước đầu tiếp cận phép chia.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000342', v_ch8_id, 'Bài 42. Số bị chia, số chia, thương',
      'Nhận biết các thành phần trong phép chia.',
      'Học sinh gọi đúng tên số bị chia, số chia, thương trong phép chia đơn giản.',
      'Nắm thuật ngữ phép chia.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000343', v_ch8_id, 'Bài 43. Bảng chia 2',
      'Ghi nhớ và sử dụng bảng chia 2.',
      'Học sinh luyện bảng chia 2 qua bài tập tính nhẩm và vận dụng.',
      'Củng cố bảng chia 2.',
      7, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000344', v_ch8_id, 'Bài 44. Bảng chia 5',
      'Ghi nhớ và sử dụng bảng chia 5.',
      'Học sinh luyện bảng chia 5 qua bài tập và tình huống thực tế.',
      'Củng cố bảng chia 5.',
      8, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000345', v_ch8_id, 'Bài 45. Luyện tập chung',
      'Củng cố kiến thức phép nhân, phép chia.',
      'Học sinh luyện tổng hợp bài tập về nhân chia và bảng nhân chia cơ bản.',
      'Ôn luyện chủ đề 8.',
      9, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000346', v_ch9_id, 'Bài 46. Khối trụ, khối cầu',
      'Nhận biết khối trụ và khối cầu.',
      'Học sinh nhận diện đặc điểm cơ bản của khối trụ, khối cầu qua vật thật.',
      'Làm quen hình khối cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000347', v_ch9_id, 'Bài 47. Luyện tập chung',
      'Củng cố kiến thức về hình khối.',
      'Học sinh luyện bài tập nhận biết và phân biệt các khối đã học.',
      'Ôn luyện chủ đề 9.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000348', v_ch10_id, 'Bài 48. Đơn vị, chục, trăm, nghìn',
      'Nhận biết cấu tạo hệ thập phân đến hàng nghìn.',
      'Học sinh xác định giá trị chữ số theo hàng đơn vị, chục, trăm, nghìn.',
      'Mở rộng kiến thức số học đến 1 000.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000349', v_ch10_id, 'Bài 49. Các số tròn trăm, tròn chục',
      'Nhận biết số tròn chục, tròn trăm.',
      'Học sinh đọc, viết và phân biệt các số tròn chục, tròn trăm trong phạm vi 1 000.',
      'Làm quen các mốc số tròn.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000350', v_ch10_id, 'Bài 50. So sánh các số tròn trăm, tròn chục',
      'So sánh số tròn chục, số tròn trăm.',
      'Học sinh sử dụng quan hệ lớn hơn, bé hơn, bằng nhau để so sánh các số tròn.',
      'Rèn kỹ năng so sánh số tròn.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000351', v_ch10_id, 'Bài 51. Số có ba chữ số',
      'Đọc, viết và nhận biết cấu tạo số có ba chữ số.',
      'Học sinh luyện đọc viết số có ba chữ số và xác định chữ số theo từng hàng.',
      'Làm quen số có ba chữ số.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000352', v_ch10_id, 'Bài 52. Viết số thành tổng các trăm, chục, đơn vị',
      'Phân tích số có ba chữ số thành tổng theo hàng.',
      'Học sinh viết số dưới dạng tổng của trăm, chục, đơn vị để hiểu cấu tạo số.',
      'Rèn kỹ năng phân tích số.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000353', v_ch10_id, 'Bài 53. So sánh các số có ba chữ số',
      'So sánh các số có ba chữ số.',
      'Học sinh so sánh số theo thứ tự hàng trăm, chục, đơn vị.',
      'Củng cố kỹ năng so sánh số có ba chữ số.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000354', v_ch10_id, 'Bài 54. Luyện tập chung',
      'Tổng hợp kiến thức số học trong phạm vi 1 000.',
      'Học sinh luyện bài tập tổng hợp về số có ba chữ số và các kỹ năng liên quan.',
      'Ôn luyện chủ đề 10.',
      7, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000355', v_ch11_id, 'Bài 55. Đề-xi-mét. Mét. Ki-lô-mét',
      'Nhận biết các đơn vị đo độ dài cơ bản.',
      'Học sinh làm quen đơn vị đề-xi-mét, mét, ki-lô-mét và mối liên hệ đơn giản giữa chúng.',
      'Mở rộng đơn vị đo độ dài.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000356', v_ch11_id, 'Bài 56. Giới thiệu tiền Việt Nam',
      'Nhận biết một số mệnh giá tiền Việt Nam thông dụng.',
      'Học sinh quan sát, nhận diện tiền Việt Nam và thực hành tính toán đơn giản với tiền.',
      'Làm quen tiền Việt Nam.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000357', v_ch11_id, 'Bài 57. Thực hành và trải nghiệm đo độ dài',
      'Vận dụng đơn vị đo độ dài trong thực hành.',
      'Học sinh thực hành đo độ dài đồ vật thực tế bằng dụng cụ phù hợp.',
      'Rèn kỹ năng đo độ dài.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000358', v_ch11_id, 'Bài 58. Luyện tập chung',
      'Củng cố kiến thức đo độ dài và tiền Việt Nam.',
      'Học sinh luyện tập tổng hợp các dạng bài về đơn vị độ dài và nhận biết tiền.',
      'Ôn luyện chủ đề 11.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000359', v_ch12_id, 'Bài 59. Phép cộng (không nhớ) trong phạm vi 1 000',
      'Thực hiện cộng không nhớ trong phạm vi 1 000.',
      'Học sinh luyện cộng số có ba chữ số không nhớ theo cột dọc.',
      'Làm quen cộng không nhớ phạm vi 1 000.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000360', v_ch12_id, 'Bài 60. Phép cộng (có nhớ) trong phạm vi 1 000',
      'Thực hiện cộng có nhớ trong phạm vi 1 000.',
      'Học sinh rèn kỹ năng cộng có nhớ với số có ba chữ số.',
      'Củng cố cộng có nhớ phạm vi 1 000.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000361', v_ch12_id, 'Bài 61. Phép trừ (không nhớ) trong phạm vi 1 000',
      'Thực hiện trừ không nhớ trong phạm vi 1 000.',
      'Học sinh luyện trừ số có ba chữ số không nhớ qua bài tập cơ bản.',
      'Làm quen trừ không nhớ phạm vi 1 000.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000362', v_ch12_id, 'Bài 62. Phép trừ (có nhớ) trong phạm vi 1 000',
      'Thực hiện trừ có nhớ trong phạm vi 1 000.',
      'Học sinh luyện kỹ năng trừ có nhớ và kiểm tra kết quả phép tính.',
      'Củng cố trừ có nhớ phạm vi 1 000.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000363', v_ch12_id, 'Bài 63. Luyện tập chung',
      'Tổng hợp cộng trừ trong phạm vi 1 000.',
      'Học sinh luyện tập tổng hợp phép cộng, phép trừ có nhớ và không nhớ.',
      'Ôn luyện chủ đề 12.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000364', v_ch13_id, 'Bài 64. Thu thập, phân loại, kiểm đếm số liệu',
      'Biết cách thu thập, phân loại và kiểm đếm số liệu đơn giản.',
      'Học sinh thực hành thu thập dữ liệu gần gũi, phân nhóm và đếm kết quả.',
      'Làm quen yếu tố thống kê cơ bản.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000365', v_ch13_id, 'Bài 65. Biểu đồ tranh',
      'Đọc và lập biểu đồ tranh đơn giản.',
      'Học sinh biểu diễn dữ liệu bằng biểu đồ tranh và rút ra nhận xét cơ bản.',
      'Làm quen biểu đồ tranh.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000366', v_ch13_id, 'Bài 66. Chắc chắn, có thể, không thể',
      'Nhận biết khả năng xảy ra của sự kiện đơn giản.',
      'Học sinh phân loại tình huống theo ba mức độ chắc chắn, có thể, không thể.',
      'Làm quen yếu tố xác suất trực quan.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000367', v_ch13_id, 'Bài 67. Thực hành và trải nghiệm thu thập, phân loại, kiểm đếm số liệu',
      'Vận dụng kỹ năng thống kê trong hoạt động thực tế.',
      'Học sinh thực hành thu thập dữ liệu, phân loại và trình bày kết quả.',
      'Rèn kỹ năng thống kê cơ bản.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000368', v_ch14_id, 'Bài 68. Ôn tập các số trong phạm vi 1 000',
      'Tổng ôn kiến thức số học đến 1 000.',
      'Học sinh ôn tập đọc viết, cấu tạo và so sánh các số trong phạm vi 1 000.',
      'Củng cố số học cuối năm.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000369', v_ch14_id, 'Bài 69. Ôn tập phép cộng, phép trừ trong phạm vi 100',
      'Tổng ôn phép cộng trừ trong phạm vi 100.',
      'Học sinh luyện bài tập tổng hợp cộng trừ trong phạm vi 100.',
      'Củng cố phép tính phạm vi 100.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000370', v_ch14_id, 'Bài 70. Ôn tập phép cộng, phép trừ trong phạm vi 1 000',
      'Tổng ôn phép cộng trừ trong phạm vi 1 000.',
      'Học sinh luyện bài tập tổng hợp cộng trừ có nhớ và không nhớ trong phạm vi 1 000.',
      'Củng cố phép tính phạm vi 1 000.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000371', v_ch14_id, 'Bài 71. Ôn tập phép nhân, phép chia',
      'Tổng ôn kiến thức phép nhân và phép chia cơ bản.',
      'Học sinh luyện tập các bảng nhân chia đã học và vận dụng giải toán.',
      'Củng cố kỹ năng nhân chia.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000372', v_ch14_id, 'Bài 72. Ôn tập hình học',
      'Tổng ôn kiến thức hình học đã học.',
      'Học sinh ôn tập nhận biết hình phẳng, hình khối và kỹ năng thực hành liên quan.',
      'Củng cố kiến thức hình học.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000373', v_ch14_id, 'Bài 73. Ôn tập đo lường',
      'Tổng ôn các nội dung đo lường.',
      'Học sinh ôn tập đơn vị đo khối lượng, dung tích, độ dài, thời gian và tiền Việt Nam.',
      'Củng cố kỹ năng đo lường.',
      6, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000374', v_ch14_id, 'Bài 74. Ôn tập kiểm đếm số liệu và lựa chọn khả năng',
      'Tổng ôn yếu tố thống kê và xác suất trực quan.',
      'Học sinh luyện kiểm đếm, biểu diễn dữ liệu đơn giản và phân loại khả năng xảy ra.',
      'Củng cố yếu tố thống kê, xác suất.',
      7, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('92000000-0000-0000-0000-000000000375', v_ch14_id, 'Bài 75. Ôn tập chung',
      'Tổng kết toàn bộ kiến thức Toán lớp 2.',
      'Học sinh hoàn thành bài tập tổng hợp cuối năm, chuẩn bị cho lớp học tiếp theo.',
      'Tổng ôn cuối năm lớp 2.',
      8, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL)
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
