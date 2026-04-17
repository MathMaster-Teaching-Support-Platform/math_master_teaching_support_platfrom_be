-- =============================================================
-- Seed Grade 10 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 9 chapters and 36 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_10_id UUID := 'a0000000-0000-0000-0000-000000000001';
    v_subject_math_g10_id UUID := 'a0000000-0000-0000-0000-000000000101';

    -- Chapters IDs (9 chương)
    v_ch1_id UUID := 'a0000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := 'a0000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := 'a0000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := 'a0000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := 'a0000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := 'a0000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := 'a0000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := 'a0000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := 'a0000000-0000-0000-0000-000000000209';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_10_id
    FROM school_grades
    WHERE grade_level = 10
    LIMIT 1;

    IF v_grade_10_id IS NULL THEN
        v_grade_10_id := 'a0000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g10_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_10'
    LIMIT 1;

    IF v_subject_math_g10_id IS NULL THEN
        v_subject_math_g10_id := 'a0000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 10
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_10_id, 10, 'Lớp 10', 'Chương trình Toán dành cho học sinh lớp 10.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 10)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g10_id,
        'Toán học',
        'TOAN_HOC_LOP_10',
        'Môn Toán học dành cho học sinh lớp 10.',
        10,
        10,
        TRUE,
        v_grade_10_id,
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
        (v_ch1_id, v_subject_math_g10_id, 'Chương I: Mệnh đề và tập hợp', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g10_id, 'Chương II: Bất phương trình và hệ bất phương trình bậc nhất hai ẩn', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g10_id, 'Chương III: Hệ thức lượng trong tam giác', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g10_id, 'Chương IV: Vectơ', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g10_id, 'Chương V: Các số đặc trưng của mẫu số liệu không ghép nhóm', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g10_id, 'Chương VI: Hàm số, đồ thị và ứng dụng', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g10_id, 'Chương VII: Phương pháp tọa độ trong mặt phẳng', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g10_id, 'Chương VIII: Đại số tổ hợp', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g10_id, 'Chương IX: Tính xác suất theo định nghĩa cổ điển', 9, v_now, v_now)
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
        ('a0000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Mệnh đề',
         'Nhận biết mệnh đề toán học và giá trị đúng sai của mệnh đề.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Tập hợp và các phép toán trên tập hợp',
         'Vận dụng các phép toán trên tập hợp.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         3, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('a0000000-0000-0000-0000-000000000304', v_ch2_id, 'Bài 3. Bất phương trình bậc nhất hai ẩn',
         'Nhận biết và biểu diễn nghiệm của bất phương trình bậc nhất hai ẩn.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000305', v_ch2_id, 'Bài 4. Hệ bất phương trình bậc nhất hai ẩn',
         'Giải và biểu diễn nghiệm của hệ bất phương trình bậc nhất hai ẩn.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000306', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         3, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('a0000000-0000-0000-0000-000000000307', v_ch3_id, 'Bài 5. Giá trị lượng giác của một góc từ 0° đến 180°',
         'Xác định giá trị lượng giác của góc từ 0° đến 180°.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000308', v_ch3_id, 'Bài 6. Hệ thức lượng trong tam giác',
         'Vận dụng hệ thức lượng trong tam giác.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000309', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         3, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('a0000000-0000-0000-0000-000000000310', v_ch4_id, 'Bài 7. Các khái niệm mở đầu',
         'Làm quen các khái niệm mở đầu về vectơ.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000311', v_ch4_id, 'Bài 8. Tổng và hiệu của hai vectơ',
         'Thực hiện phép cộng và phép trừ hai vectơ.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000312', v_ch4_id, 'Bài 9. Tích của một vectơ với một số',
         'Thực hiện phép nhân vectơ với một số.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         3, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000313', v_ch4_id, 'Bài 10. Vectơ trong mặt phẳng tọa độ',
         'Biểu diễn vectơ trong mặt phẳng tọa độ.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         4, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000314', v_ch4_id, 'Bài 11. Tích vô hướng của hai vectơ',
         'Tính tích vô hướng và vận dụng.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         5, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000315', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         6, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('a0000000-0000-0000-0000-000000000316', v_ch5_id, 'Bài 12. Số gần đúng và sai số',
         'Nhận biết số gần đúng và sai số.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000317', v_ch5_id, 'Bài 13. Các số đặc trưng đo xu thế trung tâm',
         'Tính và phân tích các số đặc trưng đo xu thế trung tâm.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000318', v_ch5_id, 'Bài 14. Các số đặc trưng đo độ phân tán',
         'Tính và phân tích các số đặc trưng đo độ phân tán.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         3, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000319', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('a0000000-0000-0000-0000-000000000320', v_ch6_id, 'Bài 15. Hàm số',
         'Nhận biết khái niệm hàm số.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000321', v_ch6_id, 'Bài 16. Hàm số bậc hai',
         'Khảo sát và biểu diễn hàm số bậc hai.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000322', v_ch6_id, 'Bài 17. Dấu của tam thức bậc hai',
         'Xét dấu tam thức bậc hai.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         3, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000323', v_ch6_id, 'Bài 18. Phương trình quy về phương trình bậc hai',
         'Giải các phương trình quy về phương trình bậc hai.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         4, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000324', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('a0000000-0000-0000-0000-000000000325', v_ch7_id, 'Bài 19. Phương trình đường thẳng',
         'Lập và nhận biết phương trình đường thẳng.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000326', v_ch7_id, 'Bài 20. Đường thẳng trong mặt phẳng tọa độ',
         'Phân tích vị trí và tính chất đường thẳng trong mặt phẳng tọa độ.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000327', v_ch7_id, 'Bài 21. Đường tròn trong mặt phẳng tọa độ',
         'Lập và nhận biết phương trình đường tròn trong mặt phẳng tọa độ.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         3, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000328', v_ch7_id, 'Bài 22. Ba đường conic',
         'Nhận biết các đường conic cơ bản.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         4, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000329', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('a0000000-0000-0000-0000-000000000330', v_ch8_id, 'Bài 23. Quy tắc đếm',
         'Vận dụng quy tắc đếm trong các bài toán tổ hợp.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000331', v_ch8_id, 'Bài 24. Hoán vị, chỉnh hợp và tổ hợp',
         'Tính toán với hoán vị, chỉnh hợp, tổ hợp.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000332', v_ch8_id, 'Bài 25. Nhị thức Newton',
         'Vận dụng công thức nhị thức Newton.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         3, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000333', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('a0000000-0000-0000-0000-000000000334', v_ch9_id, 'Bài 26. Biến cố và định nghĩa cổ điển của xác suất',
         'Nhận biết biến cố và tính xác suất theo định nghĩa cổ điển.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         1, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000335', v_ch9_id, 'Bài 27. Thực hành tính xác suất theo định nghĩa cổ điển',
         'Thực hành các bài toán xác suất cổ điển.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         2, 'PUBLISHED', v_now, v_now),
        ('a0000000-0000-0000-0000-000000000336', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
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
