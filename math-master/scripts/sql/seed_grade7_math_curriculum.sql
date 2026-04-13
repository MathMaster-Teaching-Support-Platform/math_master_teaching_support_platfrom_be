-- =============================================================
-- Seed Grade 7 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 10 chapters and 65 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_7_id UUID := '97000000-0000-0000-0000-000000000001';
    v_subject_math_g7_id UUID := '97000000-0000-0000-0000-000000000101';

    -- Chapters IDs (10 chương)
    v_ch1_id UUID := '97000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '97000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '97000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '97000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '97000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '97000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '97000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '97000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '97000000-0000-0000-0000-000000000209';
    v_ch10_id UUID := '97000000-0000-0000-0000-000000000210';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_7_id
    FROM school_grades
    WHERE grade_level = 7
    LIMIT 1;

    IF v_grade_7_id IS NULL THEN
        v_grade_7_id := '97000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g7_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_7'
    LIMIT 1;

    IF v_subject_math_g7_id IS NULL THEN
        v_subject_math_g7_id := '97000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 7
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_7_id, 7, 'Lớp 7', 'Chương trình Toán dành cho học sinh lớp 7.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 7)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g7_id,
        'Toán học',
        'TOAN_HOC_LOP_7',
        'Môn Toán học dành cho học sinh lớp 7.',
        7,
        7,
        TRUE,
        v_grade_7_id,
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
        (v_ch1_id, v_subject_math_g7_id, 'Chương I: Số hữu tỉ', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g7_id, 'Chương II: Số thực', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g7_id, 'Chương III: Góc và đường thẳng song song', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g7_id, 'Chương IV: Tam giác bằng nhau', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g7_id, 'Chương V: Thu thập và biểu diễn dữ liệu', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g7_id, 'Chương VI: Tỉ lệ thức và đại lượng tỉ lệ', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g7_id, 'Chương VII: Biểu thức đại số và đa thức một biến', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g7_id, 'Chương VIII: Làm quen với biến cố và xác suất của biến cố', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g7_id, 'Chương IX: Quan hệ giữa các yếu tố trong một tam giác', 9, v_now, v_now),
        (v_ch10_id, v_subject_math_g7_id, 'Chương X: Một số hình khối trong thực tiễn', 10, v_now, v_now)
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
        ('97000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Tập hợp các số hữu tỉ',
         'Nhận biết tập hợp số hữu tỉ.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Cộng, trừ, nhân, chia số hữu tỉ',
         'Thực hiện các phép tính với số hữu tỉ.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000303', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương I.', 'Nội dung luyện tập chương I (1)', 'Tóm tắt luyện tập chương I (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 3. Lũy thừa với số mũ tự nhiên của một số hữu tỉ',
         'Nắm quy tắc lũy thừa của số hữu tỉ.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 4. Thứ tự thực hiện các phép tính. Quy tắc chuyển vế',
         'Vận dụng thứ tự thực hiện phép tính và quy tắc chuyển vế.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000306', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương I.', 'Nội dung luyện tập chương I (2)', 'Tóm tắt luyện tập chương I (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000307', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('97000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 5. Làm quen với số thập phân vô hạn tuần hoàn',
         'Nhận biết số thập phân vô hạn tuần hoàn.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 6. Số vô tỉ. Căn bậc hai số học',
         'Làm quen số vô tỉ và căn bậc hai số học.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 7. Tập hợp các số thực',
         'Nhận biết tập hợp số thực.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000311', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức chương II.', 'Nội dung luyện tập chương II', 'Tóm tắt luyện tập chương II',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000312', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('97000000-0000-0000-0000-000000000313', v_ch3_id, 'Bài 8. Góc ở vị trí đặc biệt. Tia phân giác của một góc',
         'Nhận biết góc ở vị trí đặc biệt và tia phân giác.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000314', v_ch3_id, 'Bài 9. Hai đường thẳng song song và dấu hiệu nhận biết',
         'Nhận biết hai đường thẳng song song và dấu hiệu.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000315', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương III.', 'Nội dung luyện tập chương III (1)', 'Tóm tắt luyện tập chương III (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 10. Tiên đề Euclid. Tính chất của hai đường thẳng song song',
         'Nắm tiên đề Euclid và tính chất hai đường thẳng song song.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài 11. Định lí và chứng minh định lí',
         'Làm quen định lí và chứng minh định lí.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000318', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương III.', 'Nội dung luyện tập chương III (2)', 'Tóm tắt luyện tập chương III (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000319', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('97000000-0000-0000-0000-000000000320', v_ch4_id, 'Bài 12. Tổng các góc trong một tam giác',
         'Vận dụng định lí tổng ba góc trong tam giác.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000321', v_ch4_id, 'Bài 13. Hai tam giác bằng nhau. Trường hợp bằng nhau thứ nhất của tam giác',
         'Nhận biết hai tam giác bằng nhau và trường hợp bằng nhau thứ nhất.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000322', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 13.', 'Nội dung luyện tập chương IV (1)', 'Tóm tắt luyện tập chương IV (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000323', v_ch4_id, 'Bài 14. Trường hợp bằng nhau thứ hai và thứ ba của tam giác',
         'Vận dụng các trường hợp bằng nhau thứ hai và thứ ba.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000324', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 14.', 'Nội dung luyện tập chương IV (2)', 'Tóm tắt luyện tập chương IV (2)',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000325', v_ch4_id, 'Bài 15. Các trường hợp bằng nhau của tam giác vuông',
         'Vận dụng các trường hợp bằng nhau của tam giác vuông.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000326', v_ch4_id, 'Bài 16. Tam giác cân. Đường trung trực của đoạn thẳng',
         'Nhận biết tam giác cân và đường trung trực.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         7, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000327', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương IV.', 'Nội dung luyện tập chương IV (3)', 'Tóm tắt luyện tập chương IV (3)',
         8, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000328', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         9, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('97000000-0000-0000-0000-000000000329', v_ch5_id, 'Bài 17. Thu thập và phân loại dữ liệu',
         'Thực hiện thu thập và phân loại dữ liệu.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000330', v_ch5_id, 'Bài 18. Biểu đồ hình quạt tròn',
         'Đọc và biểu diễn dữ liệu bằng biểu đồ hình quạt tròn.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000331', v_ch5_id, 'Bài 19. Biểu đồ đoạn thẳng',
         'Đọc và biểu diễn dữ liệu bằng biểu đồ đoạn thẳng.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000332', v_ch5_id, 'Luyện tập chung',
         'Củng cố kiến thức chương V.', 'Nội dung luyện tập chương V', 'Tóm tắt luyện tập chương V',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000333', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('97000000-0000-0000-0000-000000000334', v_ch6_id, 'Bài 20. Tỉ lệ thức',
         'Nhận biết và vận dụng tỉ lệ thức.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000335', v_ch6_id, 'Bài 21. Tính chất của dãy tỉ số bằng nhau',
         'Vận dụng tính chất của dãy tỉ số bằng nhau.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000336', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VI.', 'Nội dung luyện tập chương VI (1)', 'Tóm tắt luyện tập chương VI (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000337', v_ch6_id, 'Bài 22. Đại lượng tỉ lệ thuận',
         'Nhận biết và vận dụng đại lượng tỉ lệ thuận.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000338', v_ch6_id, 'Bài 23. Đại lượng tỉ lệ nghịch',
         'Nhận biết và vận dụng đại lượng tỉ lệ nghịch.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000339', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VI.', 'Nội dung luyện tập chương VI (2)', 'Tóm tắt luyện tập chương VI (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000340', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('97000000-0000-0000-0000-000000000341', v_ch7_id, 'Bài 24. Biểu thức đại số',
         'Làm quen biểu thức đại số.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000342', v_ch7_id, 'Bài 25. Đa thức một biến',
         'Nhận biết đa thức một biến.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000343', v_ch7_id, 'Bài 26. Phép cộng và phép trừ đa thức một biến',
         'Thực hiện cộng, trừ đa thức một biến.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000344', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VII.', 'Nội dung luyện tập chương VII (1)', 'Tóm tắt luyện tập chương VII (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000345', v_ch7_id, 'Bài 27. Phép nhân đa thức một biến',
         'Thực hiện phép nhân đa thức một biến.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000346', v_ch7_id, 'Bài 28. Phép chia đa thức một biến',
         'Thực hiện phép chia đa thức một biến.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000347', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VII.', 'Nội dung luyện tập chương VII (2)', 'Tóm tắt luyện tập chương VII (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000348', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('97000000-0000-0000-0000-000000000349', v_ch8_id, 'Bài 29. Làm quen với biến cố',
         'Nhận biết biến cố trong thí nghiệm ngẫu nhiên.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000350', v_ch8_id, 'Bài 30. Làm quen với xác suất của biến cố',
         'Làm quen khái niệm xác suất của biến cố.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000351', v_ch8_id, 'Luyện tập chung',
         'Củng cố kiến thức chương VIII.', 'Nội dung luyện tập chương VIII', 'Tóm tắt luyện tập chương VIII',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000352', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('97000000-0000-0000-0000-000000000353', v_ch9_id, 'Bài 31. Quan hệ giữa góc và cạnh đối diện trong một tam giác',
         'Nhận biết quan hệ giữa góc và cạnh đối diện trong tam giác.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000354', v_ch9_id, 'Bài 32. Quan hệ giữa đường vuông góc và đường xiên',
         'Vận dụng quan hệ giữa đường vuông góc và đường xiên.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000355', v_ch9_id, 'Bài 33. Quan hệ giữa ba cạnh của một tam giác',
         'Nhận biết bất đẳng thức tam giác.', 'Nội dung bài 33', 'Tóm tắt bài 33',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000356', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương IX.', 'Nội dung luyện tập chương IX (1)', 'Tóm tắt luyện tập chương IX (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000357', v_ch9_id, 'Bài 34. Sự đồng quy của ba trung tuyến, ba đường phân giác trong một tam giác',
         'Nhận biết sự đồng quy của ba trung tuyến và ba đường phân giác.', 'Nội dung bài 34', 'Tóm tắt bài 34',
         5, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000358', v_ch9_id, 'Bài 35. Sự đồng quy của ba đường trung trực, ba đường cao trong một tam giác',
         'Nhận biết sự đồng quy của ba đường trung trực và ba đường cao.', 'Nội dung bài 35', 'Tóm tắt bài 35',
         6, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000359', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương IX.', 'Nội dung luyện tập chương IX (2)', 'Tóm tắt luyện tập chương IX (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000360', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương X
        ('97000000-0000-0000-0000-000000000361', v_ch10_id, 'Bài 36. Hình hộp chữ nhật và hình lập phương',
         'Nhận biết hình hộp chữ nhật và hình lập phương trong thực tiễn.', 'Nội dung bài 36', 'Tóm tắt bài 36',
         1, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000362', v_ch10_id, 'Luyện tập',
         'Củng cố kiến thức sau bài 36.', 'Nội dung luyện tập chương X (1)', 'Tóm tắt luyện tập chương X (1)',
         2, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000363', v_ch10_id, 'Bài 37. Hình lăng trụ đứng tam giác và hình lăng trụ đứng tứ giác',
         'Nhận biết một số hình lăng trụ đứng trong thực tiễn.', 'Nội dung bài 37', 'Tóm tắt bài 37',
         3, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000364', v_ch10_id, 'Luyện tập',
         'Củng cố kiến thức sau bài 37.', 'Nội dung luyện tập chương X (2)', 'Tóm tắt luyện tập chương X (2)',
         4, 'PUBLISHED', v_now, v_now),
        ('97000000-0000-0000-0000-000000000365', v_ch10_id, 'Bài tập cuối chương X',
         'Ôn tập tổng hợp chương X.', 'Nội dung bài tập cuối chương X', 'Tóm tắt bài tập cuối chương X',
         5, 'PUBLISHED', v_now, v_now)
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
