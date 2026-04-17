-- =============================================================
-- Seed Grade 4 Math curriculum (Vietnamese textbook structure)
-- Following "Kết nối tri thức với cuộc sống"
-- =============================================================

BEGIN;

DO $$
DECLARE
    v_now TIMESTAMPTZ := NOW();

    v_grade_4_id UUID := '94000000-0000-0000-0000-000000000001';
    v_subject_math_g4_id UUID := '94000000-0000-0000-0000-000000000101';

    -- Chapters IDs (13 Chủ đề)
    v_ch1_id UUID := '94000000-0000-0000-0000-000000000201';
    v_ch2_id UUID := '94000000-0000-0000-0000-000000000202';
    v_ch3_id UUID := '94000000-0000-0000-0000-000000000203';
    v_ch4_id UUID := '94000000-0000-0000-0000-000000000204';
    v_ch5_id UUID := '94000000-0000-0000-0000-000000000205';
    v_ch6_id UUID := '94000000-0000-0000-0000-000000000206';
    v_ch7_id UUID := '94000000-0000-0000-0000-000000000207';
    v_ch8_id UUID := '94000000-0000-0000-0000-000000000208';
    v_ch9_id UUID := '94000000-0000-0000-0000-000000000209';
    v_ch10_id UUID := '94000000-0000-0000-0000-000000000210';
    v_ch11_id UUID := '94000000-0000-0000-0000-000000000211';
    v_ch12_id UUID := '94000000-0000-0000-0000-000000000212';
    v_ch13_id UUID := '94000000-0000-0000-0000-000000000213';

BEGIN
    -- 1) School grade: Lớp 4
    INSERT INTO school_grades (id, grade_level, name, description, is_active, created_at, updated_at)
    VALUES (v_grade_4_id, 4, 'Lớp 4', 'Chương trình Toán dành cho học sinh lớp 4.', TRUE, v_now, v_now)
    ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = v_now;

    -- 2) Subject: Toán học
    INSERT INTO subjects (id, name, code, description, grade_min, grade_max, is_active, school_grade_id, created_at, updated_at)
    VALUES (v_subject_math_g4_id, 'Toán học', 'TOAN_HOC_LOP_4', 'Môn Toán học dành cho học sinh lớp 4.', 4, 4, TRUE, v_grade_4_id, v_now, v_now)
    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, updated_at = v_now;

    -- 3) Chapters (Chủ đề)
    INSERT INTO chapters (id, subject_id, title, order_index, created_at, updated_at)
    VALUES
        (v_ch1_id, v_subject_math_g4_id, 'Chủ đề 1: Ôn tập và bổ sung', 1, v_now, v_now),
        (v_ch2_id, v_subject_math_g4_id, 'Chủ đề 2: Góc và đơn vị đo góc', 2, v_now, v_now),
        (v_ch3_id, v_subject_math_g4_id, 'Chủ đề 3: Số có nhiều chữ số', 3, v_now, v_now),
        (v_ch4_id, v_subject_math_g4_id, 'Chủ đề 4: Một số đơn vị đo đại lượng', 4, v_now, v_now),
        (v_ch5_id, v_subject_math_g4_id, 'Chủ đề 5: Phép cộng và phép trừ', 5, v_now, v_now),
        (v_ch6_id, v_subject_math_g4_id, 'Chủ đề 6: Đường thẳng vuông góc. Đường thẳng song song', 6, v_now, v_now),
        (v_ch7_id, v_subject_math_g4_id, 'Chủ đề 7: Ôn tập học kì 1', 7, v_now, v_now),
        (v_ch8_id, v_subject_math_g4_id, 'Chủ đề 8: Phép nhân và phép chia', 8, v_now, v_now),
        (v_ch9_id, v_subject_math_g4_id, 'Chủ đề 9: Làm quen với yếu tố thống kê, xác suất', 9, v_now, v_now),
        (v_ch10_id, v_subject_math_g4_id, 'Chủ đề 10: Phân số', 10, v_now, v_now),
        (v_ch11_id, v_subject_math_g4_id, 'Chủ đề 11: Phép cộng, phép trừ phân số', 11, v_now, v_now),
        (v_ch12_id, v_subject_math_g4_id, 'Chủ đề 12: Phép nhân, phép chia phân số', 12, v_now, v_now),
        (v_ch13_id, v_subject_math_g4_id, 'Chủ đề 13: Ôn tập cuối năm', 13, v_now, v_now)
    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title;

    -- 4) Lessons (73 bài học)
    INSERT INTO lessons (id, chapter_id, title, learning_objectives, lesson_content, summary, order_index, status, created_at, updated_at)
    VALUES
        -- Chủ đề 1
        ('94000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Ôn tập các số đến 100 000', 'Ôn tập cấu tạo số, cách đọc viết số đến hàng chục nghìn', 'Nội dung bài 1', 'Tóm tắt bài 1', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Ôn tập các phép tính trong phạm vi 100 000', 'Củng cố 4 phép tính cộng, trừ, nhân, chia cơ bản', 'Nội dung bài 2', 'Tóm tắt bài 2', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Số chẵn, số lẻ', 'Nhận biết số chẵn và số lẻ dựa vào chữ số tận cùng', 'Nội dung bài 3', 'Tóm tắt bài 3', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Biểu thức chứa chữ', 'Làm quen với biểu thức có một, hai hoặc ba chữ', 'Nội dung bài 4', 'Tóm tắt bài 4', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Giải bài toán có ba bước tính', 'Kỹ năng phân tích và giải toán thực tế qua 3 bước', 'Nội dung bài 5', 'Tóm tắt bài 5', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 6. Luyện tập chung', 'Tổng hợp kiến thức chương 1', 'Nội dung bài 6', 'Tóm tắt bài 6', 6, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 2
        ('94000000-0000-0000-0000-000000000307', v_ch2_id, 'Bài 7. Đo góc, đơn vị đo góc', 'Sử dụng thước đo góc và đơn vị độ', 'Nội dung bài 7', 'Tóm tắt bài 7', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000308', v_ch2_id, 'Bài 8. Góc nhọn, góc tù, góc bẹt', 'Nhận biết các loại góc dựa vào góc vuông', 'Nội dung bài 8', 'Tóm tắt bài 8', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 9. Luyện tập chung', 'Thực hành nhận biết và đo góc', 'Nội dung bài 9', 'Tóm tắt bài 9', 3, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 3
        ('94000000-0000-0000-0000-000000000310', v_ch3_id, 'Bài 10. Số có sáu chữ số. Số 1 000 000', 'Mở rộng phạm vi số đến hàng trăm nghìn và hàng triệu', 'Nội dung bài 10', 'Tóm tắt bài 10', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000311', v_ch3_id, 'Bài 11. Hàng và lớp', 'Phân biệt hàng đơn vị, lớp đơn vị, lớp nghìn, lớp triệu', 'Nội dung bài 11', 'Tóm tắt bài 11', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000312', v_ch3_id, 'Bài 12. Các số trong phạm vi lớp triệu', 'Đọc và viết các số lớn đến hàng triệu', 'Nội dung bài 12', 'Tóm tắt bài 12', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000313', v_ch3_id, 'Bài 13. Làm tròn số đến hàng trăm nghìn', 'Quy tắc làm tròn các số có nhiều chữ số', 'Nội dung bài 13', 'Tóm tắt bài 13', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000314', v_ch3_id, 'Bài 14. So sánh các số có nhiều chữ số', 'So sánh dựa vào số chữ số và các hàng tương ứng', 'Nội dung bài 14', 'Tóm tắt bài 14', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000315', v_ch3_id, 'Bài 15. Làm quen với dãy số tự nhiên', 'Đặc điểm của dãy số tự nhiên', 'Nội dung bài 15', 'Tóm tắt bài 15', 6, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 16. Luyện tập chung', 'Ôn tập số có nhiều chữ số', 'Nội dung bài 16', 'Tóm tắt bài 16', 7, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 4
        ('94000000-0000-0000-0000-000000000317', v_ch4_id, 'Bài 17. Yến, tạ, tấn', 'Đơn vị đo khối lượng lớn', 'Nội dung bài 17', 'Tóm tắt bài 17', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000318', v_ch4_id, 'Bài 18. Đề-xi-mét vuông, mét vuông, mi-li-mét vuông', 'Các đơn vị đo diện tích cơ bản', 'Nội dung bài 18', 'Tóm tắt bài 18', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000319', v_ch4_id, 'Bài 19. Giây, thế kỉ', 'Đơn vị đo thời gian mở rộng', 'Nội dung bài 19', 'Tóm tắt bài 19', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000320', v_ch4_id, 'Bài 20. Thực hành trải nghiệm đo lường', 'Áp dụng đo lường vào thực tế', 'Nội dung bài 20', 'Tóm tắt bài 20', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000321', v_ch4_id, 'Bài 21. Luyện tập chung', 'Ôn tập đo lường', 'Nội dung bài 21', 'Tóm tắt bài 21', 5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 5
        ('94000000-0000-0000-0000-000000000322', v_ch5_id, 'Bài 22. Phép cộng các số có nhiều chữ số', 'Cộng không nhớ và có nhớ số lớn', 'Nội dung bài 22', 'Tóm tắt bài 22', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000323', v_ch5_id, 'Bài 23. Phép trừ các số có nhiều chữ số', 'Trừ không nhớ và có nhớ số lớn', 'Nội dung bài 23', 'Tóm tắt bài 23', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000324', v_ch5_id, 'Bài 24. Tính chất giao hoán và kết hợp của phép cộng', 'Quy tắc đổi chỗ và nhóm trong phép cộng', 'Nội dung bài 24', 'Tóm tắt bài 24', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000325', v_ch5_id, 'Bài 25. Tìm hai số khi biết tổng và hiệu', 'Công thức (Tổng + Hiệu) : 2 và (Tổng - Hiệu) : 2', 'Nội dung bài 25', 'Tóm tắt bài 25', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000326', v_ch5_id, 'Bài 26. Luyện tập chung', 'Ôn tập cộng trừ số lớn', 'Nội dung bài 26', 'Tóm tắt bài 26', 5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 6
        ('94000000-0000-0000-0000-000000000327', v_ch6_id, 'Bài 27. Hai đường thẳng vuông góc', 'Nhận biết quan hệ vuông góc qua ê-ke', 'Nội dung bài 27', 'Tóm tắt bài 27', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000328', v_ch6_id, 'Bài 28. Thực hành vẽ đường thẳng vuông góc', 'Kỹ năng vẽ hình', 'Nội dung bài 28', 'Tóm tắt bài 28', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000329', v_ch6_id, 'Bài 29. Hai đường thẳng song song', 'Nhận biết quan hệ song song', 'Nội dung bài 29', 'Tóm tắt bài 29', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000330', v_ch6_id, 'Bài 30. Thực hành vẽ đường thẳng song song', 'Sử dụng ê-ke vẽ song song', 'Nội dung bài 30', 'Tóm tắt bài 30', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000331', v_ch6_id, 'Bài 31. Hình bình hành, hình thoi', 'Đặc điểm cạnh và góc của hình bình hành, hình thoi', 'Nội dung bài 31', 'Tóm tắt bài 31', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000332', v_ch6_id, 'Bài 32. Luyện tập chung', 'Ôn tập hình học học kì 1', 'Nội dung bài 32', 'Tóm tắt bài 32', 6, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 7 (Ôn tập học kì 1)
        ('94000000-0000-0000-0000-000000000333', v_ch7_id, 'Bài 33. Ôn tập các số đến lớp triệu', 'Hệ thống hóa số học', 'Nội dung bài 33', 'Tóm tắt bài 33', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000334', v_ch7_id, 'Bài 34. Ôn tập phép cộng, phép trừ', 'Tổng hợp kỹ năng tính toán', 'Nội dung bài 34', 'Tóm tắt bài 34', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000335', v_ch7_id, 'Bài 35. Ôn tập hình học', 'Ôn tập các loại góc và quan hệ giữa các đường thẳng', 'Nội dung bài 35', 'Tóm tắt bài 35', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000336', v_ch7_id, 'Bài 36. Ôn tập đo lường', 'Quy đổi đơn vị khối lượng, diện tích, thời gian', 'Nội dung bài 36', 'Tóm tắt bài 36', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000337', v_ch7_id, 'Bài 37. Ôn tập chung', 'Tổng kết học kì 1', 'Nội dung bài 37', 'Tóm tắt bài 37', 5, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 8
        ('94000000-0000-0000-0000-000000000338', v_ch8_id, 'Bài 38. Nhân với số có một chữ số', 'Kỹ thuật đặt tính nhân', 'Nội dung bài 38', 'Tóm tắt bài 38', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000339', v_ch8_id, 'Bài 39. Chia cho số có một chữ số', 'Kỹ thuật đặt tính chia', 'Nội dung bài 39', 'Tóm tắt bài 39', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000340', v_ch8_id, 'Bài 40. Tính chất giao hoán và kết hợp của phép nhân', 'Ứng dụng tính nhanh', 'Nội dung bài 40', 'Tóm tắt bài 40', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000341', v_ch8_id, 'Bài 41. Nhân, chia với 10, 100, 1 000,...', 'Quy tắc thêm/bớt chữ số 0', 'Nội dung bài 41', 'Tóm tắt bài 41', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000342', v_ch8_id, 'Bài 42. Tính chất phân phối của phép nhân', 'Nhân một số với một tổng/hiệu', 'Nội dung bài 42', 'Tóm tắt bài 42', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000343', v_ch8_id, 'Bài 43. Nhân với số có hai chữ số', 'Kỹ thuật nhân tích riêng thứ nhất và thứ hai', 'Nội dung bài 43', 'Tóm tắt bài 43', 6, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000344', v_ch8_id, 'Bài 44. Chia cho số có hai chữ số', 'Cách ước lượng thương trong phép chia', 'Nội dung bài 44', 'Tóm tắt bài 44', 7, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000345', v_ch8_id, 'Bài 45. Thực hành ước lượng tính toán', 'Kỹ năng tính nhẩm nhanh', 'Nội dung bài 45', 'Tóm tắt bài 45', 8, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000346', v_ch8_id, 'Bài 46. Tìm số trung bình cộng', 'Cách tính trung bình cộng của nhiều số', 'Nội dung bài 46', 'Tóm tắt bài 46', 9, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000347', v_ch8_id, 'Bài 47. Bài toán rút về đơn vị', 'Các bước giải bài toán tỷ lệ thuận/nghịch cơ bản', 'Nội dung bài 47', 'Tóm tắt bài 47', 10, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000348', v_ch8_id, 'Bài 48. Luyện tập chung', 'Ôn tập nhân chia học kì 2', 'Nội dung bài 48', 'Tóm tắt bài 48', 11, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 9
        ('94000000-0000-0000-0000-000000000349', v_ch9_id, 'Bài 49. Dãy số liệu thống kê', 'Cách đọc và phân tích dãy số liệu', 'Nội dung bài 49', 'Tóm tắt bài 49', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000350', v_ch9_id, 'Bài 50. Biểu đồ cột', 'Cách xem và nhận xét biểu đồ cột', 'Nội dung bài 50', 'Tóm tắt bài 50', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000351', v_ch9_id, 'Bài 51. Số lần xuất hiện của một sự kiện', 'Làm quen với xác suất thực nghiệm', 'Nội dung bài 51', 'Tóm tắt bài 51', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000352', v_ch9_id, 'Bài 52. Luyện tập chung', 'Thực hành thống kê xác suất', 'Nội dung bài 52', 'Tóm tắt bài 52', 4, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 10
        ('94000000-0000-0000-0000-000000000353', v_ch10_id, 'Bài 53. Khái niệm phân số', 'Nhận biết tử số và mẫu số', 'Nội dung bài 53', 'Tóm tắt bài 53', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000354', v_ch10_id, 'Bài 54. Phân số và phép chia số tự nhiên', 'Biểu diễn phép chia dưới dạng phân số', 'Nội dung bài 54', 'Tóm tắt bài 54', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000355', v_ch10_id, 'Bài 55. Tính chất cơ bản của phân số', 'Rút gọn và quy đồng dựa trên tính chất nhân/chia', 'Nội dung bài 55', 'Tóm tắt bài 55', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000356', v_ch10_id, 'Bài 56. Rút gọn phân số', 'Cách đưa phân số về dạng tối giản', 'Nội dung bài 56', 'Tóm tắt bài 56', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000357', v_ch10_id, 'Bài 57. Quy đồng mẫu số các phân số', 'Cách tìm mẫu số chung', 'Nội dung bài 57', 'Tóm tắt bài 57', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000358', v_ch10_id, 'Bài 58. So sánh phân số', 'So sánh cùng mẫu và khác mẫu', 'Nội dung bài 58', 'Tóm tắt bài 58', 6, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000359', v_ch10_id, 'Bài 59. Luyện tập chung', 'Ôn tập kiến thức phân số cơ bản', 'Nội dung bài 59', 'Tóm tắt bài 59', 7, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 11
        ('94000000-0000-0000-0000-000000000360', v_ch11_id, 'Bài 60. Phép cộng phân số', 'Cộng phân số cùng mẫu và khác mẫu', 'Nội dung bài 60', 'Tóm tắt bài 60', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000361', v_ch11_id, 'Bài 61. Phép trừ phân số', 'Trừ phân số cùng mẫu và khác mẫu', 'Nội dung bài 61', 'Tóm tắt bài 61', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000362', v_ch11_id, 'Bài 62. Luyện tập chung', 'Thực hành tính cộng trừ phân số', 'Nội dung bài 62', 'Tóm tắt bài 62', 3, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 12
        ('94000000-0000-0000-0000-000000000363', v_ch12_id, 'Bài 63. Phép nhân phân số', 'Nhân tử với tử, mẫu với mẫu', 'Nội dung bài 63', 'Tóm tắt bài 63', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000364', v_ch12_id, 'Bài 64. Phép chia phân số', 'Nhân nghịch đảo phân số thứ hai', 'Nội dung bài 64', 'Tóm tắt bài 64', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000365', v_ch12_id, 'Bài 65. Tìm phân số của một số', 'Cách lấy số đó nhân với phân số', 'Nội dung bài 65', 'Tóm tắt bài 65', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000366', v_ch12_id, 'Bài 66. Luyện tập chung', 'Thực hành tính nhân chia phân số', 'Nội dung bài 66', 'Tóm tắt bài 66', 4, 'PUBLISHED', v_now, v_now),

        -- Chủ đề 13 (Ôn tập cuối năm)
        ('94000000-0000-0000-0000-000000000367', v_ch13_id, 'Bài 67. Ôn tập số tự nhiên', 'Hệ thống số học lớp 4', 'Nội dung bài 67', 'Tóm tắt bài 67', 1, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000368', v_ch13_id, 'Bài 68. Ôn tập phép tính với số tự nhiên', '4 phép tính với số lớn', 'Nội dung bài 68', 'Tóm tắt bài 68', 2, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000369', v_ch13_id, 'Bài 69. Ôn tập phân số', 'Cấu tạo và so sánh phân số', 'Nội dung bài 69', 'Tóm tắt bài 69', 3, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000370', v_ch13_id, 'Bài 70. Ôn tập phép tính với phân số', '4 phép tính với phân số', 'Nội dung bài 70', 'Tóm tắt bài 70', 4, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000371', v_ch13_id, 'Bài 71. Ôn tập hình học và đo lường', 'Hệ thống hóa hình học và đại lượng', 'Nội dung bài 71', 'Tóm tắt bài 71', 5, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000372', v_ch13_id, 'Bài 72. Ôn tập thống kê và xác suất', 'Củng cố biểu đồ và số lần xuất hiện sự kiện', 'Nội dung bài 72', 'Tóm tắt bài 72', 6, 'PUBLISHED', v_now, v_now),
        ('94000000-0000-0000-0000-000000000373', v_ch13_id, 'Bài 73. Ôn tập chung', 'Tổng kết toàn bộ chương trình Toán lớp 4', 'Nội dung bài 73', 'Tóm tắt bài 73', 7, 'PUBLISHED', v_now, v_now)
    ON CONFLICT (id) DO UPDATE SET 
        title = EXCLUDED.title, 
        learning_objectives = EXCLUDED.learning_objectives,
        lesson_content = EXCLUDED.lesson_content,
        summary = EXCLUDED.summary,
        updated_at = v_now;

END $$;

COMMIT;