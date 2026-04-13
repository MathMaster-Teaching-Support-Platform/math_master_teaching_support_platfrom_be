-- =============================================================
-- Seed Grade 3 Math curriculum (Vietnamese textbook structure)
-- Hierarchy seeded:
--   SchoolGrade (Lớp 3)
--     -> Subject (Toán học)
--       -> Chapter (Chủ đề 1 -> 16)
--         -> Lesson (Bài 1 -> 81)
-- =============================================================

BEGIN;

DO $$
DECLARE
  v_now TIMESTAMPTZ := NOW();

  v_grade_3_id UUID := '93000000-0000-0000-0000-000000000001';
  v_subject_math_g3_id UUID := '93000000-0000-0000-0000-000000000101';
  v_grade_subject_math_g3_id UUID := '93000000-0000-0000-0000-000000000151';

  -- Chapters IDs
  v_ch1_id UUID := '93000000-0000-0000-0000-000000000201';
  v_ch2_id UUID := '93000000-0000-0000-0000-000000000202';
  v_ch3_id UUID := '93000000-0000-0000-0000-000000000203';
  v_ch4_id UUID := '93000000-0000-0000-0000-000000000204';
  v_ch5_id UUID := '93000000-0000-0000-0000-000000000205';
  v_ch6_id UUID := '93000000-0000-0000-0000-000000000206';
  v_ch7_id UUID := '93000000-0000-0000-0000-000000000207';
  v_ch8_id UUID := '93000000-0000-0000-0000-000000000208';
  v_ch9_id UUID := '93000000-0000-0000-0000-000000000209';
  v_ch10_id UUID := '93000000-0000-0000-0000-000000000210';
  v_ch11_id UUID := '93000000-0000-0000-0000-000000000211';
  v_ch12_id UUID := '93000000-0000-0000-0000-000000000212';
  v_ch13_id UUID := '93000000-0000-0000-0000-000000000213';
  v_ch14_id UUID := '93000000-0000-0000-0000-000000000214';
  v_ch15_id UUID := '93000000-0000-0000-0000-000000000215';
  v_ch16_id UUID := '93000000-0000-0000-0000-000000000216';

BEGIN
  -- 1) School grade: Lớp 3
  INSERT INTO school_grades (id, grade_level, name, description, is_active, created_at, updated_at)
  VALUES (v_grade_3_id, 3, 'Lớp 3', 'Chương trình Toán dành cho học sinh lớp 3.', TRUE, v_now, v_now)
  ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = v_now;

  -- 2) Subject: Toán học (for Lớp 3)
  INSERT INTO subjects (id, name, code, description, grade_min, grade_max, is_active, school_grade_id, created_at, updated_at)
  VALUES (v_subject_math_g3_id, 'Toán học', 'TOAN_HOC_LOP_3', 'Môn Toán học dành cho học sinh lớp 3.', 3, 3, TRUE, v_grade_3_id, v_now, v_now)
  ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, updated_at = v_now;

  -- 3) Chapters (Chủ đề 1 -> 16)
  INSERT INTO chapters (id, subject_id, title, order_index, created_at, updated_at)
  VALUES
    (v_ch1_id, v_subject_math_g3_id, 'Chủ đề 1: Ôn tập và bổ sung', 1, v_now, v_now),
    (v_ch2_id, v_subject_math_g3_id, 'Chủ đề 2: Bảng nhân, bảng chia', 2, v_now, v_now),
    (v_ch3_id, v_subject_math_g3_id, 'Chủ đề 3: Làm quen với hình phẳng, hình khối', 3, v_now, v_now),
    (v_ch4_id, v_subject_math_g3_id, 'Chủ đề 4: Phép nhân, phép chia trong phạm vi 100', 4, v_now, v_now),
    (v_ch5_id, v_subject_math_g3_id, 'Chủ đề 5: Một số đơn vị đo độ dài, khối lượng, dung tích, nhiệt độ', 5, v_now, v_now),
    (v_ch6_id, v_subject_math_g3_id, 'Chủ đề 6: Phép nhân, phép chia trong phạm vi 1 000', 6, v_now, v_now),
    (v_ch7_id, v_subject_math_g3_id, 'Chủ đề 7: Ôn tập học kì 1', 7, v_now, v_now),
    (v_ch8_id, v_subject_math_g3_id, 'Chủ đề 8: Các số đến 10 000', 8, v_now, v_now),
    (v_ch9_id, v_subject_math_g3_id, 'Chủ đề 9: Chu vi, diện tích một số hình phẳng', 9, v_now, v_now),
    (v_ch10_id, v_subject_math_g3_id, 'Chủ đề 10: Cộng, trừ, nhân, chia trong phạm vi 10 000', 10, v_now, v_now),
    (v_ch11_id, v_subject_math_g3_id, 'Chủ đề 11: Các số đến 100 000', 11, v_now, v_now),
    (v_ch12_id, v_subject_math_g3_id, 'Chủ đề 12: Cộng, trừ trong phạm vi 100 000', 12, v_now, v_now),
    (v_ch13_id, v_subject_math_g3_id, 'Chủ đề 13: Xem đồng hồ. Tháng - Năm. Tiền Việt Nam', 13, v_now, v_now),
    (v_ch14_id, v_subject_math_g3_id, 'Chủ đề 14: Nhân, chia trong phạm vi 100 000', 14, v_now, v_now),
    (v_ch15_id, v_subject_math_g3_id, 'Chủ đề 15: Làm quen với yếu tố thống kê, xác suất', 15, v_now, v_now),
    (v_ch16_id, v_subject_math_g3_id, 'Chủ đề 16: Ôn tập cuối năm', 16, v_now, v_now)
  ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title;

  -- 4) Lessons (Bài học 1 -> 81)
  INSERT INTO lessons (id, chapter_id, title, order_index, status, created_at, updated_at)
  VALUES
    -- Chủ đề 1
    ('93000000-0000-0000-0000-000000000301', v_ch1_id, 'Bài 1. Ôn tập các số đến 1 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000302', v_ch1_id, 'Bài 2. Ôn tập phép cộng, phép trừ trong phạm vi 1 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000303', v_ch1_id, 'Bài 3. Tìm thành phần trong phép cộng, phép trừ', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000304', v_ch1_id, 'Bài 4. Ôn tập bảng nhân 2; 5, bảng chia 2; 5', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000305', v_ch1_id, 'Bài 5. Bảng nhân 3, bảng chia 3', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000306', v_ch1_id, 'Bài 6. Bảng nhân 4, bảng chia 4', 6, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000307', v_ch1_id, 'Bài 7. Ôn tập hình học và đo lường', 7, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000308', v_ch1_id, 'Bài 8. Luyện tập chung', 8, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 2
    ('93000000-0000-0000-0000-000000000309', v_ch2_id, 'Bài 9. Bảng nhân 6, bảng chia 6', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000310', v_ch2_id, 'Bài 10. Bảng nhân 7, bảng chia 7', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000311', v_ch2_id, 'Bài 11. Bảng nhân 8, bảng chia 8', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000312', v_ch2_id, 'Bài 12. Bảng nhân 9, bảng chia 9', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000313', v_ch2_id, 'Bài 13. Tìm thành phần trong phép nhân, phép chia', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000314', v_ch2_id, 'Bài 14. Một phần mấy', 6, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000315', v_ch2_id, 'Bài 15. Luyện tập chung', 7, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 3
    ('93000000-0000-0000-0000-000000000316', v_ch3_id, 'Bài 16. Điểm ở giữa, trung điểm của đoạn thẳng', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000317', v_ch3_id, 'Bài 17. Hình tròn. Tâm, bán kính, đường kính của hình tròn', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000318', v_ch3_id, 'Bài 18. Góc, góc vuông, góc không vuông', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000319', v_ch3_id, 'Bài 19. Hình tam giác, hình tứ giác. Hình chữ nhật, hình vuông', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000320', v_ch3_id, 'Bài 20. Thực hành vẽ góc vuông, vẽ đường tròn, hình vuông, hình chữ nhật và vẽ trang trí', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000321', v_ch3_id, 'Bài 21. Khối lập phương, khối hộp chữ nhật', 6, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000322', v_ch3_id, 'Bài 22. Luyện tập chung', 7, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 4
    ('93000000-0000-0000-0000-000000000323', v_ch4_id, 'Bài 23. Nhân số có hai chữ số với số có một chữ số', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000324', v_ch4_id, 'Bài 24. Gấp một số lên một số lần', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000325', v_ch4_id, 'Bài 25. Phép chia hết, phép chia có dư', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000326', v_ch4_id, 'Bài 26. Chia số có hai chữ số cho số có một chữ số', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000327', v_ch4_id, 'Bài 27. Giảm một số đi một số lần', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000328', v_ch4_id, 'Bài 28. Bài toán giải bằng hai bước tính', 6, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000329', v_ch4_id, 'Bài 29. Luyện tập chung', 7, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 5
    ('93000000-0000-0000-0000-000000000330', v_ch5_id, 'Bài 30. Mi-li-mét', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000331', v_ch5_id, 'Bài 31. Gam', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000332', v_ch5_id, 'Bài 32. Mi-li-lít', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000333', v_ch5_id, 'Bài 33. Nhiệt độ. Đơn vị đo nhiệt độ', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000334', v_ch5_id, 'Bài 34. Thực hành và trải nghiệm với các đơn vị mi-li-mét, gam, mi-li-lít, độ C', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000335', v_ch5_id, 'Bài 35. Luyện tập chung', 6, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 6
    ('93000000-0000-0000-0000-000000000336', v_ch6_id, 'Bài 36. Nhân số có ba chữ số với số có một chữ số', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000337', v_ch6_id, 'Bài 37. Chia số có ba chữ số cho số có một chữ số', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000338', v_ch6_id, 'Bài 38. Biểu thức số. Tính giá trị của biểu thức số', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000339', v_ch6_id, 'Bài 39. So sánh số lớn gấp mấy lần số bé', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000340', v_ch6_id, 'Bài 40. Luyện tập chung', 5, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 7
    ('93000000-0000-0000-0000-000000000341', v_ch7_id, 'Bài 41. Ôn tập phép nhân, phép chia trong phạm vi 100, 1 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000342', v_ch7_id, 'Bài 42. Ôn tập biểu thức số', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000343', v_ch7_id, 'Bài 43. Ôn tập hình học và đo lường', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000344', v_ch7_id, 'Bài 44. Ôn tập chung', 4, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 8
    ('93000000-0000-0000-0000-000000000345', v_ch8_id, 'Bài 45. Các số có bốn chữ số. Số 10 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000346', v_ch8_id, 'Bài 46. So sánh các số trong phạm vi 10 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000347', v_ch8_id, 'Bài 47. Làm quen với chữ số La Mã', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000348', v_ch8_id, 'Bài 48. Làm tròn số đến hàng chục, hàng trăm', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000349', v_ch8_id, 'Bài 49. Luyện tập chung', 5, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 9
    ('93000000-0000-0000-0000-000000000350', v_ch9_id, 'Bài 50. Chu vi hình tam giác, hình tứ giác, hình chữ nhật, hình vuông', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000351', v_ch9_id, 'Bài 51. Diện tích của một hình. Xăng-ti-mét vuông', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000352', v_ch9_id, 'Bài 52. Diện tích hình chữ nhật, diện tích hình vuông', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000353', v_ch9_id, 'Bài 53. Luyện tập chung', 4, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 10
    ('93000000-0000-0000-0000-000000000354', v_ch10_id, 'Bài 54. Phép cộng trong phạm vi 10 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000355', v_ch10_id, 'Bài 55. Phép trừ trong phạm vi 10 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000356', v_ch10_id, 'Bài 56. Nhân số có bốn chữ số với số có một chữ số', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000357', v_ch10_id, 'Bài 57. Chia số có bốn chữ số cho số có một chữ số', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000358', v_ch10_id, 'Bài 58. Luyện tập chung', 5, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 11
    ('93000000-0000-0000-0000-000000000359', v_ch11_id, 'Bài 59. Các số có năm chữ số. Số 100 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000360', v_ch11_id, 'Bài 60. So sánh các số trong phạm vi 100 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000361', v_ch11_id, 'Bài 61. Làm tròn số đến hàng nghìn, hàng chục nghìn', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000362', v_ch11_id, 'Bài 62. Luyện tập chung', 4, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 12
    ('93000000-0000-0000-0000-000000000363', v_ch12_id, 'Bài 63. Phép cộng trong phạm vi 100 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000364', v_ch12_id, 'Bài 64. Phép trừ trong phạm vi 100 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000365', v_ch12_id, 'Bài 65. Luyện tập chung', 3, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 13
    ('93000000-0000-0000-0000-000000000366', v_ch13_id, 'Bài 66. Xem đồng hồ. Tháng - năm', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000367', v_ch13_id, 'Bài 67. Thực hành xem đồng hồ, xem lịch', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000368', v_ch13_id, 'Bài 68. Tiền Việt Nam', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000369', v_ch13_id, 'Bài 69. Luyện tập chung', 4, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 14
    ('93000000-0000-0000-0000-000000000370', v_ch14_id, 'Bài 70. Nhân số có năm chữ số với số có một chữ số', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000371', v_ch14_id, 'Bài 71. Chia số có năm chữ số cho số có một chữ số', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000372', v_ch14_id, 'Bài 72. Luyện tập chung', 3, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 15
    ('93000000-0000-0000-0000-000000000373', v_ch15_id, 'Bài 73. Thu thập, phân loại, ghi chép số liệu. Bảng số liệu', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000374', v_ch15_id, 'Bài 74. Khả năng xảy ra của một sự kiện', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000375', v_ch15_id, 'Bài 75. Thực hành và trải nghiệm thu thập, phân loại, ghi chép số liệu, đọc bảng số liệu', 3, 'PUBLISHED', v_now, v_now),

    -- Chủ đề 16
    ('93000000-0000-0000-0000-000000000376', v_ch16_id, 'Bài 76. Ôn tập các số trong phạm vi 10 000, 100 000', 1, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000377', v_ch16_id, 'Bài 77. Ôn tập phép cộng, phép trừ trong phạm vi 100 000', 2, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000378', v_ch16_id, 'Bài 78. Ôn tập phép nhân, phép chia trong phạm vi 100 000', 3, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000379', v_ch16_id, 'Bài 79. Ôn tập hình học và đo lường', 4, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000380', v_ch16_id, 'Bài 80. Ôn tập bảng số liệu, khả năng xảy ra của một sự kiện', 5, 'PUBLISHED', v_now, v_now),
    ('93000000-0000-0000-0000-000000000381', v_ch16_id, 'Bài 81. Ôn tập chung', 6, 'PUBLISHED', v_now, v_now)
  ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, updated_at = v_now;

END $$;

COMMIT;