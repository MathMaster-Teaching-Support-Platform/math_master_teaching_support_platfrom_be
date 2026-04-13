-- =============================================================
-- Seed Grade 6 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 9 chapters and 67 lessons (including review/final chapter exercises)
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_6_id UUID := '96000000-0000-0000-0000-000000000001';
    v_subject_math_g6_id UUID := '96000000-0000-0000-0000-000000000101';

    -- Chapters IDs (9 chương)
    v_ch1_id UUID := '96000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '96000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '96000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '96000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '96000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '96000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '96000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '96000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '96000000-0000-0000-0000-000000000209';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_6_id
    FROM school_grades
    WHERE grade_level = 6
    LIMIT 1;

    IF v_grade_6_id IS NULL THEN
        v_grade_6_id := '96000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g6_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_6'
    LIMIT 1;

    IF v_subject_math_g6_id IS NULL THEN
        v_subject_math_g6_id := '96000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 6
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_6_id, 6, 'Lớp 6', 'Chương trình Toán dành cho học sinh lớp 6.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 6)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g6_id,
        'Toán học',
        'TOAN_HOC_LOP_6',
        'Môn Toán học dành cho học sinh lớp 6.',
        6,
        6,
        TRUE,
        v_grade_6_id,
        v_now, v_now
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
        updated_at = EXCLUDED.updated_at;

    -- 3) Chapters (Chương)
    INSERT INTO chapters (
        id, subject_id, title, order_index,
        created_at, updated_at
    )
    VALUES
        (v_ch1_id, v_subject_math_g6_id, 'Chương I: Tập hợp các số tự nhiên', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g6_id, 'Chương II: Tính chia hết trong tập hợp các số tự nhiên', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g6_id, 'Chương III: Số nguyên', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g6_id, 'Chương IV: Một số hình phẳng trong thực tiễn', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g6_id, 'Chương V: Tính đối xứng của hình phẳng trong tự nhiên', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g6_id, 'Chương VI: Phân số', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g6_id, 'Chương VII: Số thập phân', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g6_id, 'Chương VIII: Những hình hình học cơ bản', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g6_id, 'Chương IX: Dữ liệu và xác suất thực nghiệm', 9, v_now, v_now)
    ON CONFLICT (id) DO UPDATE
    SET
        subject_id = EXCLUDED.subject_id,
        title = EXCLUDED.title,
        order_index = EXCLUDED.order_index,
        updated_at = EXCLUDED.updated_at;

    -- 4) Lessons
    INSERT INTO lessons (
        id, chapter_id, title, learning_objectives, lesson_content, summary,
        order_index, status, created_at, updated_at
    )
    VALUES
        -- Chương I
        ('96000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Tập hợp',
         'Nhận biết khái niệm tập hợp và phần tử của tập hợp.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Cách ghi số tự nhiên',
         'Nắm cách viết và biểu diễn số tự nhiên.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Thứ tự trong tập hợp các số tự nhiên',
         'Hiểu thứ tự và so sánh trong tập hợp số tự nhiên.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Phép cộng và phép trừ số tự nhiên',
         'Thực hiện phép cộng, trừ trong tập hợp số tự nhiên.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Phép nhân và phép chia số tự nhiên',
         'Thực hiện phép nhân, chia trong tập hợp số tự nhiên.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000306', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương I.', 'Nội dung luyện tập chương I (1)', 'Tóm tắt luyện tập chương I (1)',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000307', v_ch1_id, 'Bài 6. Lũy thừa với số mũ tự nhiên',
         'Nắm khái niệm lũy thừa với số mũ tự nhiên.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000308', v_ch1_id, 'Bài 7. Thứ tự thực hiện các phép tính',
         'Vận dụng quy tắc thứ tự thực hiện phép tính.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         8, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000309', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương I.', 'Nội dung luyện tập chương I (2)', 'Tóm tắt luyện tập chương I (2)',
         9, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000310', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         10, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('96000000-0000-0000-0000-000000000311', v_ch2_id, 'Bài 8. Quan hệ chia hết và tính chất',
         'Nhận biết quan hệ chia hết và các tính chất cơ bản.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000312', v_ch2_id, 'Bài 9. Dấu hiệu chia hết',
         'Vận dụng dấu hiệu chia hết cho các số quen thuộc.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000313', v_ch2_id, 'Bài 10. Số nguyên tố',
         'Nhận biết số nguyên tố và hợp số.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000314', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương II.', 'Nội dung luyện tập chương II (1)', 'Tóm tắt luyện tập chương II (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000315', v_ch2_id, 'Bài 11. Ước chung. Ước chung lớn nhất',
         'Tìm ước chung và ước chung lớn nhất.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000316', v_ch2_id, 'Bài 12. Bội chung. Bội chung nhỏ nhất',
         'Tìm bội chung và bội chung nhỏ nhất.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000317', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương II.', 'Nội dung luyện tập chương II (2)', 'Tóm tắt luyện tập chương II (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000318', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('96000000-0000-0000-0000-000000000319', v_ch3_id, 'Bài 13. Tập hợp các số nguyên',
         'Làm quen tập hợp số nguyên.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000320', v_ch3_id, 'Bài 14. Phép cộng và phép trừ số nguyên',
         'Thực hiện cộng trừ số nguyên.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000321', v_ch3_id, 'Bài 15. Quy tắc dấu ngoặc',
         'Vận dụng quy tắc dấu ngoặc trong tính toán.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000322', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương III.', 'Nội dung luyện tập chương III (1)', 'Tóm tắt luyện tập chương III (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000323', v_ch3_id, 'Bài 16. Phép nhân số nguyên',
         'Thực hiện phép nhân số nguyên.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000324', v_ch3_id, 'Bài 17. Phép chia hết. Ước và bội của một số nguyên',
         'Nhận biết phép chia hết và xác định ước, bội trong số nguyên.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000325', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương III.', 'Nội dung luyện tập chương III (2)', 'Tóm tắt luyện tập chương III (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000326', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('96000000-0000-0000-0000-000000000327', v_ch4_id, 'Bài 18. Hình tam giác đều. Hình vuông. Hình lục giác đều',
         'Nhận biết một số hình phẳng thường gặp trong thực tiễn.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000328', v_ch4_id, 'Bài 19. Hình chữ nhật. Hình thoi. Hình bình hành. Hình thang cân',
         'Nhận biết các hình tứ giác và đặc điểm cơ bản.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000329', v_ch4_id, 'Bài 20. Chu vi và diện tích của một số tứ giác đã học',
         'Tính chu vi, diện tích một số tứ giác đã học.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000330', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức chương IV.', 'Nội dung luyện tập chương IV', 'Tóm tắt luyện tập chương IV',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000331', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('96000000-0000-0000-0000-000000000332', v_ch5_id, 'Bài 21. Hình có trục đối xứng',
         'Nhận biết hình có trục đối xứng.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000333', v_ch5_id, 'Bài 22. Hình có tâm đối xứng',
         'Nhận biết hình có tâm đối xứng.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000334', v_ch5_id, 'Luyện tập chung',
         'Củng cố kiến thức chương V.', 'Nội dung luyện tập chương V', 'Tóm tắt luyện tập chương V',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000335', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('96000000-0000-0000-0000-000000000336', v_ch6_id, 'Bài 23. Mở rộng phân số. Phân số bằng nhau',
         'Mở rộng khái niệm phân số và nhận biết phân số bằng nhau.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000337', v_ch6_id, 'Bài 24. So sánh phân số. Hỗn số dương',
         'So sánh phân số và làm quen hỗn số dương.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000338', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VI.', 'Nội dung luyện tập chương VI (1)', 'Tóm tắt luyện tập chương VI (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000339', v_ch6_id, 'Bài 25. Phép cộng và phép trừ phân số',
         'Thực hiện phép cộng, trừ phân số.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000340', v_ch6_id, 'Bài 26. Phép nhân và phép chia phân số',
         'Thực hiện phép nhân, chia phân số.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000341', v_ch6_id, 'Bài 27. Hai bài toán về phân số',
         'Giải một số bài toán thực tế về phân số.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000342', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VI.', 'Nội dung luyện tập chương VI (2)', 'Tóm tắt luyện tập chương VI (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000343', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('96000000-0000-0000-0000-000000000344', v_ch7_id, 'Bài 28. Số thập phân',
         'Nhận biết và biểu diễn số thập phân.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000345', v_ch7_id, 'Bài 29. Tính toán với số thập phân',
         'Thực hiện các phép tính với số thập phân.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000346', v_ch7_id, 'Bài 30. Làm tròn và ước lượng',
         'Vận dụng làm tròn và ước lượng trong tính toán.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000347', v_ch7_id, 'Bài 31. Một số bài toán về tỉ số và tỉ số phần trăm',
         'Giải một số bài toán thực tế về tỉ số và tỉ số phần trăm.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000348', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức chương VII.', 'Nội dung luyện tập chương VII', 'Tóm tắt luyện tập chương VII',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000349', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         6, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('96000000-0000-0000-0000-000000000350', v_ch8_id, 'Bài 32. Điểm và đường thẳng',
         'Nhận biết điểm và đường thẳng.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000351', v_ch8_id, 'Bài 33. Điểm nằm giữa hai điểm. Tia',
         'Nhận biết quan hệ điểm nằm giữa và khái niệm tia.', 'Nội dung bài 33', 'Tóm tắt bài 33',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000352', v_ch8_id, 'Bài 34. Đoạn thẳng. Độ dài đoạn thẳng',
         'Nhận biết đoạn thẳng và đo độ dài đoạn thẳng.', 'Nội dung bài 34', 'Tóm tắt bài 34',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000353', v_ch8_id, 'Bài 35. Trung điểm của đoạn thẳng',
         'Xác định trung điểm của đoạn thẳng.', 'Nội dung bài 35', 'Tóm tắt bài 35',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000354', v_ch8_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VIII.', 'Nội dung luyện tập chương VIII (1)', 'Tóm tắt luyện tập chương VIII (1)',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000355', v_ch8_id, 'Bài 36. Góc',
         'Nhận biết khái niệm góc.', 'Nội dung bài 36', 'Tóm tắt bài 36',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000356', v_ch8_id, 'Bài 37. Số đo góc',
         'Đo và đọc số đo góc.', 'Nội dung bài 37', 'Tóm tắt bài 37',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000357', v_ch8_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VIII.', 'Nội dung luyện tập chương VIII (2)', 'Tóm tắt luyện tập chương VIII (2)',
         8, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000358', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         9, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('96000000-0000-0000-0000-000000000359', v_ch9_id, 'Bài 38. Dữ liệu và thu thập dữ liệu',
         'Nhận biết dữ liệu và cách thu thập dữ liệu.', 'Nội dung bài 38', 'Tóm tắt bài 38',
         1, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000360', v_ch9_id, 'Bài 39. Bảng thống kê và biểu đồ tranh',
         'Đọc và lập bảng thống kê, biểu đồ tranh.', 'Nội dung bài 39', 'Tóm tắt bài 39',
         2, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000361', v_ch9_id, 'Bài 40. Biểu đồ cột',
         'Đọc và nhận xét biểu đồ cột.', 'Nội dung bài 40', 'Tóm tắt bài 40',
         3, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000362', v_ch9_id, 'Bài 41. Biểu đồ cột kép',
         'Đọc và phân tích biểu đồ cột kép.', 'Nội dung bài 41', 'Tóm tắt bài 41',
         4, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000363', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương IX.', 'Nội dung luyện tập chương IX (1)', 'Tóm tắt luyện tập chương IX (1)',
         5, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000364', v_ch9_id, 'Bài 42. Kết quả có thể và sự kiện trong trò chơi, thí nghiệm',
         'Nhận biết kết quả có thể và sự kiện trong các tình huống thực nghiệm.', 'Nội dung bài 42', 'Tóm tắt bài 42',
         6, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000365', v_ch9_id, 'Bài 43. Xác suất thực nghiệm',
         'Làm quen xác suất thực nghiệm.', 'Nội dung bài 43', 'Tóm tắt bài 43',
         7, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000366', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương IX.', 'Nội dung luyện tập chương IX (2)', 'Tóm tắt luyện tập chương IX (2)',
         8, 'PUBLISHED', v_now, v_now),
        ('96000000-0000-0000-0000-000000000367', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
         9, 'PUBLISHED', v_now, v_now)
    ON CONFLICT (id) DO UPDATE
    SET
        chapter_id = EXCLUDED.chapter_id,
        title = EXCLUDED.title,
        learning_objectives = EXCLUDED.learning_objectives,
        lesson_content = EXCLUDED.lesson_content,
        summary = EXCLUDED.summary,
        order_index = EXCLUDED.order_index,
        status = EXCLUDED.status,
        updated_at = EXCLUDED.updated_at;
END $$;

COMMIT;
