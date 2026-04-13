-- =============================================================
-- Seed Grade 1 Math curriculum - Part 2 (Chapter 6 -> 10)
-- Adds chapters and lessons from Bài 21 đến Bài 41.
-- Idempotent by fixed UUID + ON CONFLICT(id).
-- =============================================================

BEGIN;

DO $$
DECLARE
  v_now TIMESTAMPTZ := NOW();

  v_subject_math_g1_id UUID := '91000000-0000-0000-0000-000000000101';

  v_ch6_id UUID := '91000000-0000-0000-0000-000000000206';
  v_ch7_id UUID := '91000000-0000-0000-0000-000000000207';
  v_ch8_id UUID := '91000000-0000-0000-0000-000000000208';
  v_ch9_id UUID := '91000000-0000-0000-0000-000000000209';
  v_ch10_id UUID := '91000000-0000-0000-0000-000000000210';
BEGIN
  -- 1) Chapters (Chủ đề)
  INSERT INTO chapters (
    id, curriculum_id, subject_id, title, description, order_index,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    (
      v_ch6_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 6: Các số đến 100',
      'Mở rộng kiến thức số học từ phạm vi 10 đến phạm vi 100.',
      6,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch7_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 7: Độ dài và đo độ dài',
      'Làm quen so sánh độ dài, đơn vị đo và thực hành đo độ dài.',
      7,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch8_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 8: Phép cộng, phép trừ (không nhớ) trong phạm vi 100',
      'Thực hiện phép cộng và phép trừ số có hai chữ số trong phạm vi 100, không nhớ.',
      8,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch9_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 9: Thời gian, giờ và lịch',
      'Nhận biết giờ đúng, ngày trong tuần và thực hành đọc lịch.',
      9,
      v_now, NULL, v_now, NULL, NULL, NULL
    ),
    (
      v_ch10_id,
      NULL,
      v_subject_math_g1_id,
      'Chủ đề 10: Ôn tập cuối năm',
      'Tổng ôn kiến thức số học, hình học, đo lường cho cả năm học.',
      10,
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

  -- 2) Lessons (Bài học)
  INSERT INTO lessons (
    id, chapter_id, title, learning_objectives, lesson_content, summary,
    order_index, duration_minutes, difficulty, status,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  )
  VALUES
    ('91000000-0000-0000-0000-000000000322', v_ch6_id, 'Bài 21. Số có hai chữ số',
      'Nhận biết cấu tạo số có hai chữ số gồm chục và đơn vị.',
      'Học sinh làm quen với số có hai chữ số, đọc viết số và phân tích thành chục - đơn vị.',
      'Nắm cấu tạo cơ bản của số có hai chữ số.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000323', v_ch6_id, 'Bài 22. So sánh số có hai chữ số',
      'So sánh hai số có hai chữ số theo giá trị.',
      'Học sinh so sánh số có hai chữ số dựa vào hàng chục, hàng đơn vị và dùng các dấu so sánh phù hợp.',
      'Rèn kỹ năng so sánh số trong phạm vi 100.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000324', v_ch6_id, 'Bài 23. Bảng các số từ 1 đến 100',
      'Nhận biết quy luật sắp xếp số trong bảng 1 đến 100.',
      'Học sinh quan sát bảng số 1-100, tìm số theo hàng cột và nhận diện quy luật tăng giảm.',
      'Làm quen bảng số đến 100.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000325', v_ch6_id, 'Bài 24. Luyện tập chung',
      'Củng cố kiến thức chủ đề số đến 100.',
      'Tổng hợp bài tập đọc, viết, so sánh và tìm quy luật số trong phạm vi 100.',
      'Ôn luyện tổng hợp chủ đề 6.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000326', v_ch7_id, 'Bài 25. Dài hơn, ngắn hơn',
      'So sánh độ dài của các vật quen thuộc.',
      'Học sinh quan sát, ước lượng và so sánh độ dài theo các quan hệ dài hơn, ngắn hơn.',
      'Hình thành khái niệm so sánh độ dài.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000327', v_ch7_id, 'Bài 26. Đơn vị đo độ dài',
      'Làm quen với đơn vị đo độ dài cơ bản phù hợp lớp 1.',
      'Học sinh nhận biết đơn vị đo thông dụng và thực hành đo đơn giản với dụng cụ học tập.',
      'Bước đầu sử dụng đơn vị đo độ dài.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000328', v_ch7_id, 'Bài 27. Thực hành ước lượng và đo độ dài',
      'Thực hành ước lượng và đo độ dài trong tình huống thực tế.',
      'Học sinh thực hành ước lượng trước, sau đó đo và đối chiếu kết quả để rèn kỹ năng đo.',
      'Rèn kỹ năng đo độ dài qua thực hành.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000329', v_ch7_id, 'Bài 28. Luyện tập chung',
      'Củng cố kiến thức về độ dài và đo độ dài.',
      'Tổng hợp bài tập so sánh, ước lượng và đo độ dài trong các tình huống gần gũi.',
      'Ôn luyện tổng hợp chủ đề 7.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000330', v_ch8_id, 'Bài 29. Phép cộng số có hai chữ số với số có một chữ số',
      'Thực hiện phép cộng dạng hai chữ số cộng một chữ số trong phạm vi 100, không nhớ.',
      'Học sinh luyện cộng theo cột dọc hoặc theo tách số đơn giản, đảm bảo không phát sinh nhớ.',
      'Làm quen dạng cộng 2 chữ số với 1 chữ số.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000331', v_ch8_id, 'Bài 30. Phép cộng số có hai chữ số với số có hai chữ số',
      'Thực hiện phép cộng hai số có hai chữ số trong phạm vi 100, không nhớ.',
      'Học sinh luyện cộng hai chữ số theo hàng chục và hàng đơn vị trong các bài toán cơ bản.',
      'Rèn kỹ năng cộng hai số có hai chữ số.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000332', v_ch8_id, 'Bài 31. Phép trừ số có hai chữ số cho số có một chữ số',
      'Thực hiện phép trừ dạng hai chữ số trừ một chữ số trong phạm vi 100, không nhớ.',
      'Học sinh thực hành trừ theo cấu trúc hàng đơn vị và hàng chục, chú ý các bài toán không mượn.',
      'Làm quen dạng trừ 2 chữ số cho 1 chữ số.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000333', v_ch8_id, 'Bài 32. Phép trừ số có hai chữ số cho số có hai chữ số',
      'Thực hiện phép trừ hai số có hai chữ số trong phạm vi 100, không nhớ.',
      'Học sinh thực hành trừ theo từng hàng, củng cố quy tắc tính đúng và trình bày rõ ràng.',
      'Rèn kỹ năng trừ hai số có hai chữ số.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000334', v_ch8_id, 'Bài 33. Luyện tập chung',
      'Củng cố phép cộng trừ không nhớ trong phạm vi 100.',
      'Tổng hợp bài tập cộng trừ nhiều dạng nhằm tăng độ chính xác và tốc độ tính toán.',
      'Ôn luyện tổng hợp chủ đề 8.',
      5, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000335', v_ch9_id, 'Bài 34. Xem giờ đúng trên đồng hồ',
      'Nhận biết và đọc giờ đúng trên đồng hồ kim.',
      'Học sinh quan sát đồng hồ, xác định vị trí kim giờ và đọc giờ đúng trong sinh hoạt hằng ngày.',
      'Bước đầu hình thành kỹ năng xem giờ đúng.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000336', v_ch9_id, 'Bài 35. Các ngày trong tuần',
      'Nhận biết thứ tự các ngày trong tuần và tên gọi.',
      'Học sinh ghi nhớ thứ tự 7 ngày trong tuần, liên hệ với lịch sinh hoạt cá nhân.',
      'Nắm được các ngày trong tuần.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000337', v_ch9_id, 'Bài 36. Thực hành xem lịch và giờ',
      'Vận dụng kiến thức xem giờ và xem lịch trong tình huống thực tế.',
      'Học sinh thực hành đọc lịch tuần, xác định ngày và kết hợp xem giờ để xử lý bài toán gần gũi.',
      'Rèn kỹ năng sử dụng thời gian cơ bản.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000338', v_ch9_id, 'Bài 37. Luyện tập chung',
      'Củng cố kiến thức về thời gian, giờ và lịch.',
      'Tổng hợp bài tập đọc giờ đúng, xác định ngày trong tuần và sử dụng lịch đơn giản.',
      'Ôn luyện tổng hợp chủ đề 9.',
      4, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000339', v_ch10_id, 'Bài 38. Ôn tập các số và phép tính trong phạm vi 10',
      'Hệ thống lại kiến thức số học và phép tính trong phạm vi 10.',
      'Học sinh ôn đọc viết số, so sánh và thực hiện các phép cộng trừ cơ bản trong phạm vi 10.',
      'Tổng ôn số học phạm vi 10.',
      1, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000340', v_ch10_id, 'Bài 39. Ôn tập các số và phép tính trong phạm vi 100',
      'Hệ thống kiến thức số học và phép tính trong phạm vi 100.',
      'Học sinh ôn tập đọc viết số có hai chữ số, so sánh số và thực hiện cộng trừ không nhớ trong phạm vi 100.',
      'Tổng ôn số học phạm vi 100.',
      2, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000341', v_ch10_id, 'Bài 40. Ôn tập hình học và đo lường',
      'Củng cố kiến thức hình học và đo lường đã học.',
      'Học sinh ôn nhận biết hình phẳng, hình khối và vận dụng kỹ năng đo độ dài trong bài tập tổng hợp.',
      'Tổng ôn hình học và đo lường.',
      3, 35, 'BEGINNER', 'DRAFT', v_now, NULL, v_now, NULL, NULL, NULL),

    ('91000000-0000-0000-0000-000000000342', v_ch10_id, 'Bài 41. Ôn tập chung',
      'Tổng kết, củng cố toàn bộ kiến thức cuối năm.',
      'Học sinh thực hành các bài tập tổng hợp liên môn số học, hình học, thời gian và đo lường.',
      'Hoàn thiện năng lực nền tảng môn Toán lớp 1.',
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
