-- =============================================================
-- Seed Grade 11 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 9 chapters and 42 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_11_id UUID := 'b0000000-0000-0000-0000-000000000001';
    v_subject_math_g11_id UUID := 'b0000000-0000-0000-0000-000000000101';

    -- Chapters IDs (9 chương)
    v_ch1_id UUID := 'b0000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := 'b0000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := 'b0000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := 'b0000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := 'b0000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := 'b0000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := 'b0000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := 'b0000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := 'b0000000-0000-0000-0000-000000000209';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_11_id
    FROM school_grades
    WHERE grade_level = 11
    LIMIT 1;

    IF v_grade_11_id IS NULL THEN
        v_grade_11_id := 'b0000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g11_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_11'
    LIMIT 1;

    IF v_subject_math_g11_id IS NULL THEN
        v_subject_math_g11_id := 'b0000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 11
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_11_id, 11, 'Lớp 11', 'Chương trình Toán dành cho học sinh lớp 11.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 11)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g11_id,
        'Toán học',
        'TOAN_HOC_LOP_11',
        'Môn Toán học dành cho học sinh lớp 11.',
        11,
        11,
        TRUE,
        v_grade_11_id,
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
        (v_ch1_id, v_subject_math_g11_id, 'Chương I: Hàm số lượng giác và phương trình lượng giác', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g11_id, 'Chương II: Dãy số, cấp số cộng và cấp số nhân', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g11_id, 'Chương III: Các số đặc trưng đo xu thế trung tâm của mẫu số liệu ghép nhóm', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g11_id, 'Chương IV: Quan hệ song song trong không gian', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g11_id, 'Chương V: Giới hạn, hàm số liên tục', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g11_id, 'Chương VI: Hàm số mũ và hàm số lôgarit', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g11_id, 'Chương VII: Quan hệ vuông góc trong không gian', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g11_id, 'Chương VIII: Các quy tắc tính xác suất', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g11_id, 'Chương IX: Đạo hàm', 9, v_now, v_now)
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
        ('b0000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Giá trị lượng giác của góc lượng giác',
         'Nhận biết và tính giá trị lượng giác của góc lượng giác.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Công thức lượng giác',
         'Vận dụng các công thức lượng giác cơ bản.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Hàm số lượng giác',
         'Nhận biết và khảo sát các hàm số lượng giác cơ bản.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Phương trình lượng giác cơ bản',
         'Giải các phương trình lượng giác cơ bản.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         4, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('b0000000-0000-0000-0000-000000000306', v_ch2_id, 'Bài 5. Dãy số',
         'Nhận biết khái niệm dãy số.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000307', v_ch2_id, 'Bài 6. Cấp số cộng',
         'Nhận biết và vận dụng cấp số cộng.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 7. Cấp số nhân',
         'Nhận biết và vận dụng cấp số nhân.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('b0000000-0000-0000-0000-000000000310', v_ch3_id, 'Bài 8. Mẫu số liệu ghép nhóm',
         'Nhận biết và lập mẫu số liệu ghép nhóm.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000311', v_ch3_id, 'Bài 9. Các số đặc trưng đo xu thế trung tâm',
         'Tính các số đặc trưng đo xu thế trung tâm của mẫu ghép nhóm.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000312', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         3, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('b0000000-0000-0000-0000-000000000313', v_ch4_id, 'Bài 10. Đường thẳng và mặt phẳng trong không gian',
         'Nhận biết các yếu tố hình học không gian cơ bản.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000314', v_ch4_id, 'Bài 11. Hai đường thẳng song song',
         'Nhận biết và vận dụng tính chất hai đường thẳng song song trong không gian.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000315', v_ch4_id, 'Bài 12. Đường thẳng và mặt phẳng song song',
         'Nhận biết và vận dụng quan hệ song song giữa đường thẳng và mặt phẳng.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000316', v_ch4_id, 'Bài 13. Hai mặt phẳng song song',
         'Nhận biết và vận dụng quan hệ song song giữa hai mặt phẳng.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         4, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000317', v_ch4_id, 'Bài 14. Phép chiếu song song',
         'Nhận biết và vận dụng phép chiếu song song.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         5, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000318', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         6, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('b0000000-0000-0000-0000-000000000319', v_ch5_id, 'Bài 15. Giới hạn của dãy số',
         'Nhận biết giới hạn của dãy số.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000320', v_ch5_id, 'Bài 16. Giới hạn của hàm số',
         'Nhận biết giới hạn của hàm số.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000321', v_ch5_id, 'Bài 17. Hàm số liên tục',
         'Nhận biết hàm số liên tục.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000322', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('b0000000-0000-0000-0000-000000000323', v_ch6_id, 'Bài 18. Lũy thừa với số mũ thực',
         'Nhận biết và vận dụng lũy thừa với số mũ thực.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000324', v_ch6_id, 'Bài 19. Lôgarit',
         'Nhận biết và vận dụng khái niệm lôgarit.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000325', v_ch6_id, 'Bài 20. Hàm số mũ và hàm số lôgarit',
         'Khảo sát và vận dụng hàm số mũ, hàm số lôgarit.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000326', v_ch6_id, 'Bài 21. Phương trình, bất phương trình mũ và lôgarit',
         'Giải phương trình và bất phương trình mũ, lôgarit cơ bản.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         4, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000327', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('b0000000-0000-0000-0000-000000000328', v_ch7_id, 'Bài 22. Hai đường thẳng vuông góc',
         'Nhận biết và vận dụng quan hệ vuông góc giữa hai đường thẳng.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000329', v_ch7_id, 'Bài 23. Đường thẳng vuông góc với mặt phẳng',
         'Nhận biết và vận dụng quan hệ vuông góc giữa đường thẳng và mặt phẳng.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000330', v_ch7_id, 'Bài 24. Phép chiếu vuông góc. Góc giữa đường thẳng và mặt phẳng',
         'Vận dụng phép chiếu vuông góc và góc giữa đường thẳng với mặt phẳng.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000331', v_ch7_id, 'Bài 25. Hai mặt phẳng vuông góc',
         'Nhận biết và vận dụng quan hệ vuông góc giữa hai mặt phẳng.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         4, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000332', v_ch7_id, 'Bài 26. Khoảng cách',
         'Tính khoảng cách trong không gian.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         5, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000333', v_ch7_id, 'Bài 27. Thể tích',
         'Tính thể tích một số khối hình học cơ bản.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         6, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000334', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('b0000000-0000-0000-0000-000000000335', v_ch8_id, 'Bài 28. Biến cố hợp, biến cố giao, biến cố độc lập',
         'Nhận biết các phép toán và quan hệ giữa các biến cố.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000336', v_ch8_id, 'Bài 29. Công thức cộng xác suất',
         'Vận dụng công thức cộng xác suất.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000337', v_ch8_id, 'Bài 30. Công thức nhân xác suất cho hai biến cố độc lập',
         'Vận dụng công thức nhân xác suất cho biến cố độc lập.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000338', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('b0000000-0000-0000-0000-000000000339', v_ch9_id, 'Bài 31. Định nghĩa và ý nghĩa của đạo hàm',
         'Nhận biết định nghĩa và ý nghĩa hình học, vật lí của đạo hàm.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         1, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000340', v_ch9_id, 'Bài 32. Các quy tắc tính đạo hàm',
         'Vận dụng các quy tắc tính đạo hàm cơ bản.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         2, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000341', v_ch9_id, 'Bài 33. Đạo hàm cấp hai',
         'Tính và vận dụng đạo hàm cấp hai.', 'Nội dung bài 33', 'Tóm tắt bài 33',
         3, 'PUBLISHED', v_now, v_now),
        ('b0000000-0000-0000-0000-000000000342', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
         4, 'PUBLISHED', v_now, v_now)
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
