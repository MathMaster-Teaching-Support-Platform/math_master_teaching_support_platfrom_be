-- =============================================================
-- Seed Grade 5 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- Includes 12 chapters and 75 lessons
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_5_id UUID := '95000000-0000-0000-0000-000000000001';
    v_subject_math_g5_id UUID := '95000000-0000-0000-0000-000000000101';

    -- Chapters IDs (12 chủ đề)
    v_ch1_id UUID := '95000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '95000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '95000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '95000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '95000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '95000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '95000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '95000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '95000000-0000-0000-0000-000000000209';
    v_ch10_id UUID := '95000000-0000-0000-0000-000000000210';
    v_ch11_id UUID := '95000000-0000-0000-0000-000000000211';
    v_ch12_id UUID := '95000000-0000-0000-0000-000000000212';
BEGIN
    -- Reuse existing IDs when unique keys already exist with different UUIDs.
    SELECT id INTO v_grade_5_id
    FROM school_grades
    WHERE grade_level = 5
    LIMIT 1;

    IF v_grade_5_id IS NULL THEN
        v_grade_5_id := '95000000-0000-0000-0000-000000000001';
    END IF;

    SELECT id INTO v_subject_math_g5_id
    FROM subjects
    WHERE code = 'TOAN_HOC_LOP_5'
    LIMIT 1;

    IF v_subject_math_g5_id IS NULL THEN
        v_subject_math_g5_id := '95000000-0000-0000-0000-000000000101';
    END IF;

    -- 1) School grade: Lớp 5
    INSERT INTO school_grades (
        id, grade_level, name, description, is_active,
        created_at, updated_at
    )
    VALUES (
        v_grade_5_id, 5, 'Lớp 5', 'Chương trình Toán dành cho học sinh lớp 5.', TRUE,
        v_now, v_now
    )
    ON CONFLICT (id) DO UPDATE
    SET
        grade_level = EXCLUDED.grade_level,
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        is_active = EXCLUDED.is_active,
        updated_at = EXCLUDED.updated_at;

    -- 2) Subject: Toán học (for Lớp 5)
    INSERT INTO subjects (
        id, name, code, description, grade_min, grade_max,
        is_active, school_grade_id,
        created_at, updated_at
    )
    VALUES (
        v_subject_math_g5_id,
        'Toán học',
        'TOAN_HOC_LOP_5',
        'Môn Toán học dành cho học sinh lớp 5.',
        5,
        5,
        TRUE,
        v_grade_5_id,
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

    -- 3) Chapters (Chủ đề)
    INSERT INTO chapters (
        id, subject_id, title, order_index,
        created_at, updated_at
    )
    VALUES
        (v_ch1_id, v_subject_math_g5_id, 'Chủ đề 1: Ôn tập và bổ sung', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g5_id, 'Chủ đề 2: Số thập phân', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g5_id, 'Chủ đề 3: Một số đơn vị đo diện tích', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g5_id, 'Chủ đề 4: Các phép tính với số thập phân', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g5_id, 'Chủ đề 5: Một số hình phẳng. Chu vi và diện tích', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g5_id, 'Chủ đề 6: Ôn tập học kì I', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g5_id, 'Chủ đề 7: Tỉ số và các bài toán liên quan', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g5_id, 'Chủ đề 8: Thể tích. Đơn vị đo thể tích', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g5_id, 'Chủ đề 9: Diện tích và thể tích của một số hình khối', 9, v_now, v_now),
        (v_ch10_id, v_subject_math_g5_id, 'Chủ đề 10: Số đo thời gian, vận tốc. Các bài toán liên quan đến chuyển động đều', 10, v_now, v_now),
        (v_ch11_id, v_subject_math_g5_id, 'Chủ đề 11: Một số yếu tố thống kê và xác suất', 11, v_now, v_now),
        (v_ch12_id, v_subject_math_g5_id, 'Chủ đề 12: Ôn tập cuối năm', 12, v_now, v_now)
    ON CONFLICT (id) DO UPDATE
    SET
        subject_id = EXCLUDED.subject_id,
        title = EXCLUDED.title,
        order_index = EXCLUDED.order_index,
        updated_at = EXCLUDED.updated_at;

    -- 4) Lessons (75 bài học)
    INSERT INTO lessons (
        id, chapter_id, title, learning_objectives, lesson_content, summary,
        order_index, status, created_at, updated_at
    )
    VALUES
        -- Chủ đề 1
        ('95000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Ôn tập số tự nhiên',
         'Ôn tập kiến thức về số tự nhiên.', 'Nội dung bài 1', 'Tóm tắt bài 1',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Ôn tập các phép tính với số tự nhiên',
         'Củng cố các phép tính với số tự nhiên.', 'Nội dung bài 2', 'Tóm tắt bài 2',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Ôn tập phân số',
         'Ôn tập khái niệm và thao tác với phân số.', 'Nội dung bài 3', 'Tóm tắt bài 3',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Phân số thập phân',
         'Nhận biết phân số thập phân.', 'Nội dung bài 4', 'Tóm tắt bài 4',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Ôn tập các phép tính với phân số',
         'Củng cố phép tính với phân số.', 'Nội dung bài 5', 'Tóm tắt bài 5',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 6. Cộng, trừ hai phân số',
         'Thực hiện cộng, trừ hai phân số.', 'Nội dung bài 6', 'Tóm tắt bài 6',
         6, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000307', v_ch1_id, 'Bài 7. Hỗn số',
         'Nhận biết và sử dụng hỗn số.', 'Nội dung bài 7', 'Tóm tắt bài 7',
         7, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000308', v_ch1_id, 'Bài 8. Ôn tập hình học và đo lường',
         'Ôn tập kiến thức hình học và đo lường.', 'Nội dung bài 8', 'Tóm tắt bài 8',
         8, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000309', v_ch1_id, 'Bài 9. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 1.', 'Nội dung bài 9', 'Tóm tắt bài 9',
         9, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 2
        ('95000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 10. Khái niệm số thập phân',
         'Làm quen khái niệm số thập phân.', 'Nội dung bài 10', 'Tóm tắt bài 10',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000311', v_ch2_id, 'Bài 11. So sánh các số thập phân',
         'So sánh các số thập phân.', 'Nội dung bài 11', 'Tóm tắt bài 11',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000312', v_ch2_id, 'Bài 12. Viết số đo đại lượng dưới dạng số thập phân',
         'Chuyển đổi số đo đại lượng sang số thập phân.', 'Nội dung bài 12', 'Tóm tắt bài 12',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000313', v_ch2_id, 'Bài 13. Làm tròn số thập phân',
         'Thực hiện làm tròn số thập phân.', 'Nội dung bài 13', 'Tóm tắt bài 13',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000314', v_ch2_id, 'Bài 14. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 2.', 'Nội dung bài 14', 'Tóm tắt bài 14',
         5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 3
        ('95000000-0000-0000-0000-000000000315', v_ch3_id, 'Bài 15. Ki-lô-mét vuông. Héc-ta',
         'Làm quen đơn vị đo diện tích lớn.', 'Nội dung bài 15', 'Tóm tắt bài 15',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 16. Các đơn vị đo diện tích',
         'Củng cố các đơn vị đo diện tích.', 'Nội dung bài 16', 'Tóm tắt bài 16',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài 17. Thực hành và trải nghiệm với một số đơn vị đo đại lượng',
         'Thực hành sử dụng các đơn vị đo đại lượng.', 'Nội dung bài 17', 'Tóm tắt bài 17',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000318', v_ch3_id, 'Bài 18. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 3.', 'Nội dung bài 18', 'Tóm tắt bài 18',
         4, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 4
        ('95000000-0000-0000-0000-000000000319', v_ch4_id, 'Bài 19. Phép cộng số thập phân',
         'Thực hiện phép cộng số thập phân.', 'Nội dung bài 19', 'Tóm tắt bài 19',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000320', v_ch4_id, 'Bài 20. Phép trừ số thập phân',
         'Thực hiện phép trừ số thập phân.', 'Nội dung bài 20', 'Tóm tắt bài 20',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000321', v_ch4_id, 'Bài 21. Phép nhân số thập phân',
         'Thực hiện phép nhân số thập phân.', 'Nội dung bài 21', 'Tóm tắt bài 21',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000322', v_ch4_id, 'Bài 22. Phép chia số thập phân',
         'Thực hiện phép chia số thập phân.', 'Nội dung bài 22', 'Tóm tắt bài 22',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000323', v_ch4_id, 'Bài 23. Nhân, chia số thập phân với 10; 100; 1 000;... hoặc với 0,1; 0,01; 0,001;...',
         'Vận dụng quy tắc nhân chia với 10, 100, 1 000 và 0,1; 0,01; 0,001.', 'Nội dung bài 23', 'Tóm tắt bài 23',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000324', v_ch4_id, 'Bài 24. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 4.', 'Nội dung bài 24', 'Tóm tắt bài 24',
         6, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 5
        ('95000000-0000-0000-0000-000000000325', v_ch5_id, 'Bài 25. Hình tam giác. Diện tích hình tam giác',
         'Nhận biết và tính diện tích hình tam giác.', 'Nội dung bài 25', 'Tóm tắt bài 25',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000326', v_ch5_id, 'Bài 26. Hình thang. Diện tích hình thang',
         'Nhận biết và tính diện tích hình thang.', 'Nội dung bài 26', 'Tóm tắt bài 26',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000327', v_ch5_id, 'Bài 27. Đường tròn. Chu vi và diện tích hình tròn',
         'Nhận biết đường tròn và tính chu vi, diện tích hình tròn.', 'Nội dung bài 27', 'Tóm tắt bài 27',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000328', v_ch5_id, 'Bài 28. Thực hành và trải nghiệm đo, vẽ, lắp ghép, tạo hình',
         'Thực hành các hoạt động hình học.', 'Nội dung bài 28', 'Tóm tắt bài 28',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000329', v_ch5_id, 'Bài 29. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 5.', 'Nội dung bài 29', 'Tóm tắt bài 29',
         5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 6
        ('95000000-0000-0000-0000-000000000330', v_ch6_id, 'Bài 30. Ôn tập số thập phân',
         'Ôn tập kiến thức số thập phân.', 'Nội dung bài 30', 'Tóm tắt bài 30',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000331', v_ch6_id, 'Bài 31. Ôn tập các phép tính với số thập phân',
         'Củng cố phép tính với số thập phân.', 'Nội dung bài 31', 'Tóm tắt bài 31',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000332', v_ch6_id, 'Bài 32. Ôn tập một số hình phẳng',
         'Ôn tập kiến thức hình phẳng.', 'Nội dung bài 32', 'Tóm tắt bài 32',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000333', v_ch6_id, 'Bài 33. Ôn tập diện tích, chu vi một số hình phẳng',
         'Ôn tập chu vi, diện tích các hình phẳng.', 'Nội dung bài 33', 'Tóm tắt bài 33',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000334', v_ch6_id, 'Bài 34. Ôn tập đo lường',
         'Ôn tập kiến thức đo lường.', 'Nội dung bài 34', 'Tóm tắt bài 34',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000335', v_ch6_id, 'Bài 35. Ôn tập chung',
         'Tổng hợp kiến thức học kì I.', 'Nội dung bài 35', 'Tóm tắt bài 35',
         6, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 7
        ('95000000-0000-0000-0000-000000000336', v_ch7_id, 'Bài 36. Tỉ số. Tỉ số phần trăm',
         'Làm quen tỉ số và tỉ số phần trăm.', 'Nội dung bài 36', 'Tóm tắt bài 36',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000337', v_ch7_id, 'Bài 37. Tỉ lệ bản đồ và ứng dụng',
         'Hiểu và vận dụng tỉ lệ bản đồ.', 'Nội dung bài 37', 'Tóm tắt bài 37',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000338', v_ch7_id, 'Bài 38. Tìm hai số khi biết tổng và tỉ số của hai số đó',
         'Giải bài toán tổng và tỉ số.', 'Nội dung bài 38', 'Tóm tắt bài 38',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000339', v_ch7_id, 'Bài 39. Tìm hai số khi biết hiệu và tỉ số của hai số đó',
         'Giải bài toán hiệu và tỉ số.', 'Nội dung bài 39', 'Tóm tắt bài 39',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000340', v_ch7_id, 'Bài 40. Tìm tỉ số phần trăm của hai số',
         'Tính tỉ số phần trăm của hai số.', 'Nội dung bài 40', 'Tóm tắt bài 40',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000341', v_ch7_id, 'Bài 41. Tìm giá trị phần trăm của một số',
         'Tính giá trị phần trăm của một số.', 'Nội dung bài 41', 'Tóm tắt bài 41',
         6, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000342', v_ch7_id, 'Bài 42. Máy tính cầm tay',
         'Sử dụng máy tính cầm tay trong tính toán.', 'Nội dung bài 42', 'Tóm tắt bài 42',
         7, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000343', v_ch7_id, 'Bài 43. Thực hành và trải nghiệm sử dụng máy tính cầm tay',
         'Thực hành sử dụng máy tính cầm tay.', 'Nội dung bài 43', 'Tóm tắt bài 43',
         8, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000344', v_ch7_id, 'Bài 44. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 7.', 'Nội dung bài 44', 'Tóm tắt bài 44',
         9, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 8
        ('95000000-0000-0000-0000-000000000345', v_ch8_id, 'Bài 45. Thể tích của một hình',
         'Làm quen khái niệm thể tích.', 'Nội dung bài 45', 'Tóm tắt bài 45',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000346', v_ch8_id, 'Bài 46. Xăng-ti-mét khối. Đề-xi-mét khối',
         'Làm quen đơn vị đo thể tích cơ bản.', 'Nội dung bài 46', 'Tóm tắt bài 46',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000347', v_ch8_id, 'Bài 47. Mét khối',
         'Làm quen đơn vị mét khối.', 'Nội dung bài 47', 'Tóm tắt bài 47',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000348', v_ch8_id, 'Bài 48. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 8.', 'Nội dung bài 48', 'Tóm tắt bài 48',
         4, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 9
        ('95000000-0000-0000-0000-000000000349', v_ch9_id, 'Bài 49. Hình khai triển của hình lập phương, hình hộp chữ nhật và hình trụ',
         'Nhận biết hình khai triển của một số hình khối.', 'Nội dung bài 49', 'Tóm tắt bài 49',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000350', v_ch9_id, 'Bài 50. Diện tích xung quanh và diện tích toàn phần của hình hộp chữ nhật',
         'Tính diện tích xung quanh và diện tích toàn phần hình hộp chữ nhật.', 'Nội dung bài 50', 'Tóm tắt bài 50',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000351', v_ch9_id, 'Bài 51. Diện tích xung quanh và diện tích toàn phần của hình lập phương',
         'Tính diện tích xung quanh và diện tích toàn phần hình lập phương.', 'Nội dung bài 51', 'Tóm tắt bài 51',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000352', v_ch9_id, 'Bài 52. Thể tích của hình hộp chữ nhật',
         'Tính thể tích hình hộp chữ nhật.', 'Nội dung bài 52', 'Tóm tắt bài 52',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000353', v_ch9_id, 'Bài 53. Thể tích của hình lập phương',
         'Tính thể tích hình lập phương.', 'Nội dung bài 53', 'Tóm tắt bài 53',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000354', v_ch9_id, 'Bài 54. Thực hành tính toán và ước lượng thể tích một số hình khối',
         'Thực hành tính và ước lượng thể tích.', 'Nội dung bài 54', 'Tóm tắt bài 54',
         6, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000355', v_ch9_id, 'Bài 55. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 9.', 'Nội dung bài 55', 'Tóm tắt bài 55',
         7, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 10
        ('95000000-0000-0000-0000-000000000356', v_ch10_id, 'Bài 56. Các đơn vị đo thời gian',
         'Ôn tập và mở rộng các đơn vị đo thời gian.', 'Nội dung bài 56', 'Tóm tắt bài 56',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000357', v_ch10_id, 'Bài 57. Cộng, trừ số đo thời gian',
         'Thực hiện cộng trừ số đo thời gian.', 'Nội dung bài 57', 'Tóm tắt bài 57',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000358', v_ch10_id, 'Bài 58. Nhân, chia số đo thời gian với một số',
         'Thực hiện nhân chia số đo thời gian với một số.', 'Nội dung bài 58', 'Tóm tắt bài 58',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000359', v_ch10_id, 'Bài 59. Vận tốc của một chuyển động đều',
         'Hiểu khái niệm vận tốc chuyển động đều.', 'Nội dung bài 59', 'Tóm tắt bài 59',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000360', v_ch10_id, 'Bài 60. Quãng đường, thời gian của một chuyển động đều',
         'Giải bài toán quãng đường, thời gian trong chuyển động đều.', 'Nội dung bài 60', 'Tóm tắt bài 60',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000361', v_ch10_id, 'Bài 61. Thực hành tính toán và ước lượng về vận tốc, quãng đường, thời gian trong chuyển động đều',
         'Thực hành bài toán chuyển động đều.', 'Nội dung bài 61', 'Tóm tắt bài 61',
         6, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000362', v_ch10_id, 'Bài 62. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 10.', 'Nội dung bài 62', 'Tóm tắt bài 62',
         7, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 11
        ('95000000-0000-0000-0000-000000000363', v_ch11_id, 'Bài 63. Thu thập, phân loại, sắp xếp các số liệu',
         'Thu thập, phân loại và sắp xếp số liệu.', 'Nội dung bài 63', 'Tóm tắt bài 63',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000364', v_ch11_id, 'Bài 64. Biểu đồ hình quạt tròn',
         'Đọc và nhận xét biểu đồ hình quạt tròn.', 'Nội dung bài 64', 'Tóm tắt bài 64',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000365', v_ch11_id, 'Bài 65. Tỉ số của số lần lặp lại một sự kiện so với tổng số lần thực hiện',
         'Nhận biết tỉ số lặp lại của một sự kiện.', 'Nội dung bài 65', 'Tóm tắt bài 65',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000366', v_ch11_id, 'Bài 66. Thực hành và trải nghiệm thu thập, phân tích, biểu diễn các số liệu thống kê',
         'Thực hành thu thập, phân tích và biểu diễn số liệu.', 'Nội dung bài 66', 'Tóm tắt bài 66',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000367', v_ch11_id, 'Bài 67. Luyện tập chung',
         'Tổng hợp kiến thức chủ đề 11.', 'Nội dung bài 67', 'Tóm tắt bài 67',
         5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 12
        ('95000000-0000-0000-0000-000000000368', v_ch12_id, 'Bài 68. Ôn tập số tự nhiên, phân số, số thập phân',
         'Ôn tập tổng hợp số tự nhiên, phân số, số thập phân.', 'Nội dung bài 68', 'Tóm tắt bài 68',
         1, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000369', v_ch12_id, 'Bài 69. Ôn tập các phép tính với số tự nhiên, phân số, số thập phân',
         'Ôn tập các phép tính với nhiều dạng số.', 'Nội dung bài 69', 'Tóm tắt bài 69',
         2, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000370', v_ch12_id, 'Bài 70. Ôn tập tỉ số, tỉ số phần trăm',
         'Ôn tập tỉ số và tỉ số phần trăm.', 'Nội dung bài 70', 'Tóm tắt bài 70',
         3, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000371', v_ch12_id, 'Bài 71. Ôn tập hình học',
         'Ôn tập kiến thức hình học.', 'Nội dung bài 71', 'Tóm tắt bài 71',
         4, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000372', v_ch12_id, 'Bài 72. Ôn tập đo lường',
         'Ôn tập kiến thức đo lường.', 'Nội dung bài 72', 'Tóm tắt bài 72',
         5, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000373', v_ch12_id, 'Bài 73. Ôn tập toán chuyển động đều',
         'Ôn tập bài toán chuyển động đều.', 'Nội dung bài 73', 'Tóm tắt bài 73',
         6, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000374', v_ch12_id, 'Bài 74. Ôn tập một số yếu tố thống kê và xác suất',
         'Ôn tập yếu tố thống kê và xác suất.', 'Nội dung bài 74', 'Tóm tắt bài 74',
         7, 'PUBLISHED', v_now, v_now),
        ('95000000-0000-0000-0000-000000000375', v_ch12_id, 'Bài 75. Ôn tập chung',
         'Tổng kết toàn bộ kiến thức Toán lớp 5.', 'Nội dung bài 75', 'Tóm tắt bài 75',
         8, 'PUBLISHED', v_now, v_now)
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
