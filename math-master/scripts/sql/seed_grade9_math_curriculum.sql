-- =============================================================
-- Seed Grade 9 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 10 chapters and 56 lesson entries
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_9_id UUID := '99000000-0000-0000-0000-000000000001';
    v_subject_math_g9_id UUID := '99000000-0000-0000-0000-000000000101';

    -- Chapters IDs (10 chương)
    v_ch1_id UUID := '99000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '99000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '99000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '99000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '99000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '99000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '99000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '99000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '99000000-0000-0000-0000-000000000209';
    v_ch10_id UUID := '99000000-0000-0000-0000-000000000210';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_9_id
    FROM school_grades
    WHERE grade_level = 9
    LIMIT 1;

    IF v_grade_9_id IS NULL THEN
        v_grade_9_id := '99000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g9_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_9'
    LIMIT 1;

    IF v_subject_math_g9_id IS NULL THEN
        v_subject_math_g9_id := '99000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 9
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_9_id, 9, 'Lớp 9', 'Chương trình Toán dành cho học sinh lớp 9.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 9)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g9_id,
        'Toán học',
        'TOAN_HOC_LOP_9',
        'Môn Toán học dành cho học sinh lớp 9.',
        9,
        9,
        TRUE,
        v_grade_9_id,
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
        (v_ch1_id, v_subject_math_g9_id, 'Chương I: Phương trình và hệ hai phương trình bậc nhất hai ẩn', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g9_id, 'Chương II: Phương trình và bất phương trình bậc nhất một ẩn', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g9_id, 'Chương III: Căn bậc hai và căn bậc ba', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g9_id, 'Chương IV: Hệ thức lượng trong tam giác vuông', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g9_id, 'Chương V: Đường tròn', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g9_id, 'Chương VI: Hàm số y = ax2 (a khác 0). Phương trình bậc hai một ẩn', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g9_id, 'Chương VII: Tần số và tần số tương đối', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g9_id, 'Chương VIII: Xác suất của biến cố trong một số mô hình xác suất đơn giản', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g9_id, 'Chương IX: Đường tròn ngoại tiếp và đường tròn nội tiếp', 9, v_now, v_now),
        (v_ch10_id, v_subject_math_g9_id, 'Chương X: Một số hình khối trong thực tiễn', 10, v_now, v_now)
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
        ('99000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Khái niệm phương trình và hệ hai phương trình bậc nhất hai ẩn',
         'Nhận biết phương trình bậc nhất hai ẩn và hệ hai phương trình bậc nhất hai ẩn.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Giải hệ hai phương trình bậc nhất hai ẩn',
         'Giải hệ hai phương trình bậc nhất hai ẩn bằng các phương pháp cơ bản.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000303', v_ch1_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 2.', 'Nội dung luyện tập chương I', 'Tóm tắt luyện tập chương I',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 3. Giải bài toán bằng cách lập hệ phương trình',
         'Giải bài toán thực tế bằng cách lập hệ phương trình.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài tập cuối chương I',
         'Ôn tập tổng hợp chương I.', 'Nội dung bài tập cuối chương I', 'Tóm tắt bài tập cuối chương I',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương II
        ('99000000-0000-0000-0000-000000000306', v_ch2_id, 'Bài 4. Phương trình quy về phương trình bậc nhất một ẩn',
         'Giải các phương trình quy về phương trình bậc nhất một ẩn.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000307', v_ch2_id, 'Bài 5. Bất đẳng thức và tính chất',
         'Nhận biết bất đẳng thức và vận dụng các tính chất.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000308', v_ch2_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương II.', 'Nội dung luyện tập chương II', 'Tóm tắt luyện tập chương II',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 6. Bất phương trình bậc nhất một ẩn',
         'Giải và biểu diễn nghiệm bất phương trình bậc nhất một ẩn.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài tập cuối chương II',
         'Ôn tập tổng hợp chương II.', 'Nội dung bài tập cuối chương II', 'Tóm tắt bài tập cuối chương II',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương III
        ('99000000-0000-0000-0000-000000000311', v_ch3_id, 'Bài 7. Căn bậc hai và căn thức bậc hai',
         'Nhận biết căn bậc hai và căn thức bậc hai.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000312', v_ch3_id, 'Bài 8. Khai căn bậc hai với phép nhân và phép chia',
         'Vận dụng quy tắc khai căn bậc hai với phép nhân, phép chia.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000313', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 8.', 'Nội dung luyện tập chương III (1)', 'Tóm tắt luyện tập chương III (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000314', v_ch3_id, 'Bài 9. Biến đổi đơn giản và rút gọn biểu thức chứa căn thức bậc hai',
         'Rút gọn biểu thức chứa căn thức bậc hai bằng các biến đổi đơn giản.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000315', v_ch3_id, 'Bài 10. Căn bậc ba và căn thức bậc ba',
         'Nhận biết căn bậc ba và căn thức bậc ba.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         5, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000316', v_ch3_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương III.', 'Nội dung luyện tập chương III (2)', 'Tóm tắt luyện tập chương III (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài tập cuối chương III',
         'Ôn tập tổng hợp chương III.', 'Nội dung bài tập cuối chương III', 'Tóm tắt bài tập cuối chương III',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương IV
        ('99000000-0000-0000-0000-000000000318', v_ch4_id, 'Bài 11. Tỉ số lượng giác của góc nhọn',
         'Nhận biết và vận dụng tỉ số lượng giác của góc nhọn.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000319', v_ch4_id, 'Bài 12. Một số hệ thức giữa cạnh, góc trong tam giác vuông và ứng dụng',
         'Vận dụng các hệ thức lượng trong tam giác vuông.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000320', v_ch4_id, 'Luyện tập chung',
         'Củng cố kiến thức chương IV.', 'Nội dung luyện tập chương IV', 'Tóm tắt luyện tập chương IV',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000321', v_ch4_id, 'Bài tập cuối chương IV',
         'Ôn tập tổng hợp chương IV.', 'Nội dung bài tập cuối chương IV', 'Tóm tắt bài tập cuối chương IV',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương V
        ('99000000-0000-0000-0000-000000000322', v_ch5_id, 'Bài 13. Mở đầu về đường tròn',
         'Nhận biết các yếu tố cơ bản của đường tròn.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000323', v_ch5_id, 'Bài 14. Cung và dây của một đường tròn',
         'Nhận biết quan hệ giữa cung và dây trong đường tròn.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000324', v_ch5_id, 'Bài 15. Độ dài của cung tròn. Diện tích hình quạt tròn và hình vành khuyên',
         'Tính độ dài cung tròn và diện tích hình quạt tròn, vành khuyên.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000325', v_ch5_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương V.', 'Nội dung luyện tập chương V (1)', 'Tóm tắt luyện tập chương V (1)',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000326', v_ch5_id, 'Bài 16. Vị trí tương đối của đường thẳng và đường tròn',
         'Nhận biết vị trí tương đối giữa đường thẳng và đường tròn.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         5, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000327', v_ch5_id, 'Bài 17. Vị trí tương đối của hai đường tròn',
         'Nhận biết vị trí tương đối của hai đường tròn.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         6, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000328', v_ch5_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương V.', 'Nội dung luyện tập chương V (2)', 'Tóm tắt luyện tập chương V (2)',
         7, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000329', v_ch5_id, 'Bài tập cuối chương V',
         'Ôn tập tổng hợp chương V.', 'Nội dung bài tập cuối chương V', 'Tóm tắt bài tập cuối chương V',
         8, 'PUBLISHED', v_now, v_now),

        -- Chương VI
        ('99000000-0000-0000-0000-000000000330', v_ch6_id, 'Bài 18. Hàm số y = ax2 (a ≠ 0)',
         'Nhận biết hàm số y = ax2 và đồ thị.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000331', v_ch6_id, 'Bài 19. Phương trình bậc hai một ẩn',
         'Giải phương trình bậc hai một ẩn.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000332', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức sau bài 19.', 'Nội dung luyện tập chương VI (1)', 'Tóm tắt luyện tập chương VI (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000333', v_ch6_id, 'Bài 20. Định lí Viète và ứng dụng',
         'Vận dụng định lí Viète trong giải toán.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000334', v_ch6_id, 'Bài 21. Giải bài toán bằng cách lập phương trình',
         'Giải bài toán thực tế bằng cách lập phương trình bậc hai.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         5, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000335', v_ch6_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương VI.', 'Nội dung luyện tập chương VI (2)', 'Tóm tắt luyện tập chương VI (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000336', v_ch6_id, 'Bài tập cuối chương VI',
         'Ôn tập tổng hợp chương VI.', 'Nội dung bài tập cuối chương VI', 'Tóm tắt bài tập cuối chương VI',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương VII
        ('99000000-0000-0000-0000-000000000337', v_ch7_id, 'Bài 22. Bảng tần số và biểu đồ tần số',
         'Lập bảng tần số và biểu đồ tần số.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000338', v_ch7_id, 'Bài 23. Bảng tần số tương đối và biểu đồ tần số tương đối',
         'Lập bảng tần số tương đối và biểu đồ tần số tương đối.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000339', v_ch7_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương VII.', 'Nội dung luyện tập chương VII', 'Tóm tắt luyện tập chương VII',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000340', v_ch7_id, 'Bài 24. Bảng tần số, tần số tương đối ghép nhóm và biểu đồ',
         'Lập bảng tần số ghép nhóm và biểu đồ tương ứng.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000341', v_ch7_id, 'Bài tập cuối chương VII',
         'Ôn tập tổng hợp chương VII.', 'Nội dung bài tập cuối chương VII', 'Tóm tắt bài tập cuối chương VII',
         5, 'PUBLISHED', v_now, v_now),

        -- Chương VIII
        ('99000000-0000-0000-0000-000000000342', v_ch8_id, 'Bài 25. Phép thử ngẫu nhiên và không gian mẫu',
         'Nhận biết phép thử ngẫu nhiên và không gian mẫu.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000343', v_ch8_id, 'Bài 26. Xác suất của biến cố liên quan tới phép thử',
         'Tính xác suất của biến cố liên quan tới phép thử.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000344', v_ch8_id, 'Luyện tập chung',
         'Củng cố kiến thức chương VIII.', 'Nội dung luyện tập chương VIII', 'Tóm tắt luyện tập chương VIII',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000345', v_ch8_id, 'Bài tập cuối chương VIII',
         'Ôn tập tổng hợp chương VIII.', 'Nội dung bài tập cuối chương VIII', 'Tóm tắt bài tập cuối chương VIII',
         4, 'PUBLISHED', v_now, v_now),

        -- Chương IX
        ('99000000-0000-0000-0000-000000000346', v_ch9_id, 'Bài 27. Góc nội tiếp',
         'Nhận biết góc nội tiếp và các tính chất.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000347', v_ch9_id, 'Bài 28. Đường tròn ngoại tiếp và đường tròn nội tiếp của một tam giác',
         'Nhận biết đường tròn ngoại tiếp và nội tiếp tam giác.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000348', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần đầu chương IX.', 'Nội dung luyện tập chương IX (1)', 'Tóm tắt luyện tập chương IX (1)',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000349', v_ch9_id, 'Bài 29. Tứ giác nội tiếp',
         'Nhận biết tứ giác nội tiếp và tính chất.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         4, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000350', v_ch9_id, 'Bài 30. Đa giác đều',
         'Nhận biết đa giác đều và các yếu tố liên quan.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         5, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000351', v_ch9_id, 'Luyện tập chung',
         'Củng cố kiến thức phần cuối chương IX.', 'Nội dung luyện tập chương IX (2)', 'Tóm tắt luyện tập chương IX (2)',
         6, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000352', v_ch9_id, 'Bài tập cuối chương IX',
         'Ôn tập tổng hợp chương IX.', 'Nội dung bài tập cuối chương IX', 'Tóm tắt bài tập cuối chương IX',
         7, 'PUBLISHED', v_now, v_now),

        -- Chương X
        ('99000000-0000-0000-0000-000000000353', v_ch10_id, 'Bài 31. Hình trụ và hình nón',
         'Nhận biết hình trụ và hình nón trong thực tiễn.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         1, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000354', v_ch10_id, 'Bài 32. Hình cầu',
         'Nhận biết hình cầu trong thực tiễn.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         2, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000355', v_ch10_id, 'Luyện tập chung',
         'Củng cố kiến thức chương X.', 'Nội dung luyện tập chương X', 'Tóm tắt luyện tập chương X',
         3, 'PUBLISHED', v_now, v_now),
        ('99000000-0000-0000-0000-000000000356', v_ch10_id, 'Bài tập cuối chương X',
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
