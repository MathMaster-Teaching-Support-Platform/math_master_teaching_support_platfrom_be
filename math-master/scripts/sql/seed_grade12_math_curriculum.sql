-- =============================================================
-- Seed Grade 12 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 6 chapters and 25 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_12_id UUID := 'c0000000-0000-0000-0000-000000000001';
    v_subject_math_g12_id UUID := 'c0000000-0000-0000-0000-000000000101';

    -- Chapters IDs (6 chương)
    v_ch1_id UUID := 'c0000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := 'c0000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := 'c0000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := 'c0000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := 'c0000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := 'c0000000-0000-0000-0000-000000000206';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_12_id
    FROM school_grades
    WHERE grade_level = 12
    LIMIT 1;

    IF v_grade_12_id IS NULL THEN
        v_grade_12_id := 'c0000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g12_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_12'
    LIMIT 1;

    IF v_subject_math_g12_id IS NULL THEN
        v_subject_math_g12_id := 'c0000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 12
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_12_id, 12, 'Lớp 12', 'Chương trình Toán dành cho học sinh lớp 12.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 12)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g12_id,
        'Toán học',
        'TOAN_HOC_LOP_12',
        'Môn Toán học dành cho học sinh lớp 12.',
        12,
        12,
        TRUE,
        v_grade_12_id,
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
        (v_ch1_id, v_subject_math_g12_id, 'Chương I: Ứng dụng đạo hàm để khảo sát và vẽ đồ thị hàm số', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g12_id, 'Chương II: Vectơ và hệ trục tọa độ trong không gian', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g12_id, 'Chương III: Các số đặc trưng đo mức độ phân tán của mẫu số liệu ghép nhóm', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g12_id, 'Chương IV: Nguyên hàm và tích phân', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g12_id, 'Chương V: Phương pháp tọa độ trong không gian', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g12_id, 'Chương VI: Xác suất có điều kiện', 6, v_now, v_now)
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
        ('c0000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Tính đơn điệu và cực trị của hàm số',
         'Khảo sát tính đơn điệu và cực trị của hàm số bằng đạo hàm.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Giá trị lớn nhất và giá trị nhỏ nhất của hàm số',
         'Tìm giá trị lớn nhất và nhỏ nhất của hàm số trên miền xác định phù hợp.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Đường tiệm cận của đồ thị hàm số',
         'Xác định các đường tiệm cận của đồ thị hàm số.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         3, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Khảo sát sự biến thiên và vẽ đồ thị của hàm số',
         'Khảo sát sự biến thiên và vẽ đồ thị hàm số.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         4, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Ứng dụng đạo hàm để giải quyết một số vấn đề liên quan đến thực tiễn',
         'Vận dụng đạo hàm vào bài toán thực tiễn.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         5, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         6, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('c0000000-0000-0000-0000-000000000307', v_ch2_id, 'Bài 6. Vectơ trong không gian',
         'Nhận biết và biểu diễn vectơ trong không gian.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 7. Hệ trục tọa độ trong không gian',
         'Thiết lập hệ trục tọa độ trong không gian.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 8. Biểu thức tọa độ của các phép toán vectơ',
         'Vận dụng biểu thức tọa độ của các phép toán vectơ.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         3, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('c0000000-0000-0000-0000-000000000311', v_ch3_id, 'Bài 9. Khoảng biến thiên và khoảng tứ phân vị',
         'Tính khoảng biến thiên và khoảng tứ phân vị cho mẫu số liệu ghép nhóm.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000312', v_ch3_id, 'Bài 10. Phương sai và độ lệch chuẩn',
         'Tính phương sai và độ lệch chuẩn của mẫu số liệu ghép nhóm.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000313', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         3, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('c0000000-0000-0000-0000-000000000314', v_ch4_id, 'Bài 11. Nguyên hàm',
         'Nhận biết và tính nguyên hàm cơ bản.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000315', v_ch4_id, 'Bài 12. Tích phân',
         'Nhận biết và tính tích phân cơ bản.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000316', v_ch4_id, 'Bài 13. Ứng dụng hình học của tích phân',
         'Vận dụng tích phân vào tính diện tích và thể tích trong hình học.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         3, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000317', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('c0000000-0000-0000-0000-000000000318', v_ch5_id, 'Bài 14. Phương trình mặt phẳng',
         'Lập và nhận biết phương trình mặt phẳng.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000319', v_ch5_id, 'Bài 15. Phương trình đường thẳng trong không gian',
         'Lập và nhận biết phương trình đường thẳng trong không gian.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000320', v_ch5_id, 'Bài 16. Công thức tính góc trong không gian',
         'Vận dụng công thức tính góc trong không gian.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         3, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000321', v_ch5_id, 'Bài 17. Phương trình mặt cầu',
         'Lập và nhận biết phương trình mặt cầu.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         4, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000322', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('c0000000-0000-0000-0000-000000000323', v_ch6_id, 'Bài 18. Xác suất có điều kiện',
         'Nhận biết và tính xác suất có điều kiện.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         1, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000324', v_ch6_id, 'Bài 19. Công thức xác suất toàn phần và công thức Bayes',
         'Vận dụng công thức xác suất toàn phần và công thức Bayes.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         2, 'PUBLISHED', v_now, v_now),
        ('c0000000-0000-0000-0000-000000000325', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         3, 'PUBLISHED', v_now, v_now)
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
