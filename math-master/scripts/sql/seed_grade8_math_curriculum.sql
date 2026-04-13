-- =============================================================
-- Seed Grade 8 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 10 chapters and 66 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_8_id UUID := '98000000-0000-0000-0000-000000000001';
    v_subject_math_g8_id UUID := '98000000-0000-0000-0000-000000000101';

    -- Chapters IDs (10 chương)
    v_ch1_id UUID := '98000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '98000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '98000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '98000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '98000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '98000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '98000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '98000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '98000000-0000-0000-0000-000000000209';
    v_ch10_id UUID := '98000000-0000-0000-0000-000000000210';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_8_id
    FROM school_grades
    WHERE grade_level = 8
    LIMIT 1;

    IF v_grade_8_id IS NULL THEN
        v_grade_8_id := '98000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g8_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_8'
    LIMIT 1;

    IF v_subject_math_g8_id IS NULL THEN
        v_subject_math_g8_id := '98000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 8
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_8_id, 8, 'Lớp 8', 'Chương trình Toán dành cho học sinh lớp 8.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 8)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g8_id,
        'Toán học',
        'TOAN_HOC_LOP_8',
        'Môn Toán học dành cho học sinh lớp 8.',
        8,
        8,
        TRUE,
        v_grade_8_id,
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
        (v_ch1_id, v_subject_math_g8_id, 'Chương I: Đa thức', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g8_id, 'Chương II: Hằng đẳng thức đáng nhớ và ứng dụng', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g8_id, 'Chương III: Tứ giác', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g8_id, 'Chương IV: Định lí Thales', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g8_id, 'Chương V: Dữ liệu và biểu đồ', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g8_id, 'Chương VI: Phân thức đại số', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g8_id, 'Chương VII: Phương trình bậc nhất và hàm số bậc nhất', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g8_id, 'Chương VIII: Mở đầu về tính xác suất của biến cố', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g8_id, 'Chương IX: Tam giác đồng dạng', 9, v_now, v_now),
        (v_ch10_id, v_subject_math_g8_id, 'Chương X: Một số hình khối trong thực tiễn', 10, v_now, v_now)
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
        ('98000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Đơn thức',
         'Nhận biết và biểu diễn đơn thức.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Đa thức',
         'Nhận biết và biểu diễn đa thức.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Phép cộng và phép trừ đa thức',
         'Thực hiện phép cộng, trừ đa thức.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000304', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương I.', 'Nội dung luyện tập chương I (1)', 'Tóm tắt luyện tập chương I (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 4. Phép nhân đa thức',
         'Thực hiện phép nhân đa thức.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 5. Phép chia đa thức cho đơn thức',
         'Thực hiện phép chia đa thức cho đơn thức.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000307', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương I.', 'Nội dung luyện tập chương I (2)', 'Tóm tắt luyện tập chương I (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000308', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('98000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 6. Hiệu hai bình phương. Bình phương của một tổng hay một hiệu',
         'Vận dụng các hằng đẳng thức liên quan bình phương.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 7. Lập phương của một tổng hay một hiệu',
         'Vận dụng hằng đẳng thức lập phương của tổng, hiệu.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000311', v_ch2_id, 'Bài 8. Tổng và hiệu hai lập phương',
         'Vận dụng hằng đẳng thức tổng, hiệu hai lập phương.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000312', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương II.', 'Nội dung luyện tập chương II (1)', 'Tóm tắt luyện tập chương II (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000313', v_ch2_id, 'Bài 9. Phân tích đa thức thành nhân tử',
         'Phân tích đa thức thành nhân tử bằng các phương pháp cơ bản.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000314', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương II.', 'Nội dung luyện tập chương II (2)', 'Tóm tắt luyện tập chương II (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000315', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('98000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 10. Tứ giác',
         'Nhận biết tứ giác và các tính chất cơ bản.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài 11. Hình thang cân',
         'Nhận biết hình thang cân và các tính chất.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000318', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 11.', 'Nội dung luyện tập chương III (1)', 'Tóm tắt luyện tập chương III (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000319', v_ch3_id, 'Bài 12. Hình bình hành',
         'Nhận biết hình bình hành và các tính chất.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000320', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức về hình bình hành.', 'Nội dung luyện tập chương III (2)', 'Tóm tắt luyện tập chương III (2)',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000321', v_ch3_id, 'Bài 13. Hình chữ nhật',
         'Nhận biết hình chữ nhật và các tính chất.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000322', v_ch3_id, 'Bài 14. Hình thoi và hình vuông',
         'Nhận biết hình thoi, hình vuông và các tính chất.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         7, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000323', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương III.', 'Nội dung luyện tập chương III (3)', 'Tóm tắt luyện tập chương III (3)',
         8, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000324', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         9, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('98000000-0000-0000-0000-000000000325', v_ch4_id, 'Bài 15. Định lí Thales trong tam giác',
         'Vận dụng định lí Thales trong tam giác.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000326', v_ch4_id, 'Bài 16. Đường trung bình của tam giác',
         'Nhận biết và vận dụng tính chất đường trung bình của tam giác.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000327', v_ch4_id, 'Bài 17. Tính chất đường phân giác của tam giác',
         'Vận dụng tính chất đường phân giác của tam giác.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000328', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức chương IV.', 'Nội dung luyện tập chương IV', 'Tóm tắt luyện tập chương IV',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000329', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('98000000-0000-0000-0000-000000000330', v_ch5_id, 'Bài 18. Thu thập và phân loại dữ liệu',
         'Thu thập và phân loại dữ liệu thống kê.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000331', v_ch5_id, 'Bài 19. Biểu diễn dữ liệu bằng bảng, biểu đồ',
         'Biểu diễn dữ liệu bằng bảng và biểu đồ phù hợp.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000332', v_ch5_id, 'Bài 20. Phân tích số liệu thống kê dựa vào biểu đồ',
         'Phân tích và rút ra nhận xét từ biểu đồ thống kê.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000333', v_ch5_id, 'Luyện tập chung',
         'Củng cố kiến thức chương V.', 'Nội dung luyện tập chương V', 'Tóm tắt luyện tập chương V',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000334', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('98000000-0000-0000-0000-000000000335', v_ch6_id, 'Bài 21. Phân thức đại số',
         'Nhận biết phân thức đại số.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000336', v_ch6_id, 'Bài 22. Tính chất cơ bản của phân thức đại số',
         'Vận dụng tính chất cơ bản của phân thức đại số.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000337', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VI.', 'Nội dung luyện tập chương VI (1)', 'Tóm tắt luyện tập chương VI (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000338', v_ch6_id, 'Bài 23. Phép cộng và phép trừ phân thức đại số',
         'Thực hiện phép cộng, trừ phân thức đại số.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000339', v_ch6_id, 'Bài 24. Phép nhân và phép chia phân thức đại số',
         'Thực hiện phép nhân, chia phân thức đại số.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000340', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VI.', 'Nội dung luyện tập chương VI (2)', 'Tóm tắt luyện tập chương VI (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000341', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('98000000-0000-0000-0000-000000000342', v_ch7_id, 'Bài 25. Phương trình bậc nhất một ẩn',
         'Nhận biết và giải phương trình bậc nhất một ẩn.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000343', v_ch7_id, 'Bài 26. Giải bài toán bằng cách lập phương trình',
         'Giải bài toán thực tế bằng cách lập phương trình.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000344', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VII.', 'Nội dung luyện tập chương VII (1)', 'Tóm tắt luyện tập chương VII (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000345', v_ch7_id, 'Bài 27. Khái niệm hàm số và đồ thị của hàm số',
         'Làm quen khái niệm hàm số và đồ thị.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000346', v_ch7_id, 'Bài 28. Hàm số bậc nhất và đồ thị của hàm số bậc nhất',
         'Nhận biết hàm số bậc nhất và đồ thị.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000347', v_ch7_id, 'Bài 29. Hệ số góc của đường thẳng',
         'Nhận biết hệ số góc của đường thẳng.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000348', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VII.', 'Nội dung luyện tập chương VII (2)', 'Tóm tắt luyện tập chương VII (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000349', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('98000000-0000-0000-0000-000000000350', v_ch8_id, 'Bài 30. Kết quả có thể và kết quả thuận lợi',
         'Nhận biết kết quả có thể và kết quả thuận lợi.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000351', v_ch8_id, 'Bài 31. Cách tính xác suất của biến cố bằng tỉ số',
         'Tính xác suất của biến cố bằng tỉ số.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000352', v_ch8_id, 'Bài 32. Mối liên hệ giữa xác suất thực nghiệm với xác suất và ứng dụng',
         'Nhận biết mối liên hệ giữa xác suất thực nghiệm và xác suất.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000353', v_ch8_id, 'Luyện tập chung',
         'Củng cố kiến thức chương VIII.', 'Nội dung luyện tập chương VIII', 'Tóm tắt luyện tập chương VIII',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000354', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('98000000-0000-0000-0000-000000000355', v_ch9_id, 'Bài 33. Hai tam giác đồng dạng',
         'Nhận biết hai tam giác đồng dạng.', 'Nội dung bài 33', 'Tóm tắt bài 33',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000356', v_ch9_id, 'Bài 34. Ba trường hợp đồng dạng của hai tam giác',
         'Vận dụng ba trường hợp đồng dạng của tam giác.', 'Nội dung bài 34', 'Tóm tắt bài 34',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000357', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương IX.', 'Nội dung luyện tập chương IX (1)', 'Tóm tắt luyện tập chương IX (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000358', v_ch9_id, 'Bài 35. Định lí Pythagore và ứng dụng',
         'Vận dụng định lí Pythagore trong giải toán.', 'Nội dung bài 35', 'Tóm tắt bài 35',
         4, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000359', v_ch9_id, 'Bài 36. Các trường hợp đồng dạng của hai tam giác vuông',
         'Vận dụng các trường hợp đồng dạng của tam giác vuông.', 'Nội dung bài 36', 'Tóm tắt bài 36',
         5, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000360', v_ch9_id, 'Bài 37. Hình đồng dạng',
         'Nhận biết hình đồng dạng.', 'Nội dung bài 37', 'Tóm tắt bài 37',
         6, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000361', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương IX.', 'Nội dung luyện tập chương IX (2)', 'Tóm tắt luyện tập chương IX (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000362', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương X
        ('98000000-0000-0000-0000-000000000363', v_ch10_id, 'Bài 38. Hình chóp tam giác đều',
         'Nhận biết hình chóp tam giác đều.', 'Nội dung bài 38', 'Tóm tắt bài 38',
         1, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000364', v_ch10_id, 'Bài 39. Hình chóp tứ giác đều',
         'Nhận biết hình chóp tứ giác đều.', 'Nội dung bài 39', 'Tóm tắt bài 39',
         2, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000365', v_ch10_id, 'Luyện tập chung',
         'Củng cố kiến thức chương X.', 'Nội dung luyện tập chương X', 'Tóm tắt luyện tập chương X',
         3, 'PUBLISHED', v_now, v_now),
        ('98000000-0000-0000-0000-000000000366', v_ch10_id, 'Bài tập cuối chương X',
         'Ôn tập tổng hợp chương X.', 'Nội dung bài tập cuối chương X', 'Tóm tắt bài tập cuối chương X',
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
