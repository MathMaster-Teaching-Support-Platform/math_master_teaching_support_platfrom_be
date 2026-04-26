package com.fptu.math_master.enums;

/**
 * Standardized tags for question templates and questions.
 * Provides consistent categorization across the system.
 */
public enum QuestionTag {
    // Algebra
    LINEAR_EQUATIONS("Phương trình bậc nhất"),
    QUADRATIC_EQUATIONS("Phương trình bậc hai"),
    SYSTEMS_OF_EQUATIONS("Hệ phương trình"),
    INEQUALITIES("Bất phương trình"),
    POLYNOMIALS("Đa thức"),
    RATIONAL_EXPRESSIONS("Biểu thức hữu tỉ"),
    EXPONENTS_LOGARITHMS("Lũy thừa và logarit"),

    // Geometry
    TRIANGLES("Tam giác"),
    CIRCLES("Đường tròn"),
    VECTORS("Vectơ"),
    COORDINATE_GEOMETRY("Hình học tọa độ"),
    SOLID_GEOMETRY("Hình học không gian"),
    TRANSFORMATIONS("Phép biến hình"),
    ANGLES("Góc"),
    PARALLEL_PERPENDICULAR("Song song và vuông góc"),

    // Calculus
    LIMITS("Giới hạn"),
    DERIVATIVES("Đạo hàm"),
    INTEGRALS("Tích phân"),
    SEQUENCES_SERIES("Dãy số và chuỗi"),
    FUNCTIONS("Hàm số"),

    // Statistics & Probability
    PROBABILITY("Xác suất"),
    STATISTICS("Thống kê"),
    DATA_ANALYSIS("Phân tích dữ liệu"),
    DISTRIBUTIONS("Phân phối xác suất"),

    // Trigonometry
    TRIGONOMETRIC_FUNCTIONS("Hàm lượng giác"),
    TRIGONOMETRIC_EQUATIONS("Phương trình lượng giác"),
    TRIGONOMETRIC_IDENTITIES("Công thức lượng giác"),

    // Number Theory
    DIVISIBILITY("Tính chia hết"),
    PRIME_NUMBERS("Số nguyên tố"),
    GCD_LCM("Ước chung và bội chung"),
    MODULAR_ARITHMETIC("Số học modulo"),

    // Combinatorics
    COMBINATORICS("Tổ hợp"),
    PERMUTATIONS("Hoán vị"),
    BINOMIAL_THEOREM("Nhị thức Newton"),

    // Logic & Sets
    LOGIC("Lô-gic"),
    SETS("Tập hợp"),
    RELATIONS("Quan hệ"),

    // Applied Math
    WORD_PROBLEMS("Bài toán thực tế"),
    OPTIMIZATION("Tối ưu hóa"),
    FINANCIAL_MATH("Toán tài chính"),
    PHYSICS_APPLICATIONS("Ứng dụng vật lý"),

    // Other
    PROOF_TECHNIQUES("Kỹ thuật chứng minh"),
    PROBLEM_SOLVING("Giải quyết vấn đề"),
    MENTAL_MATH("Tính nhẩm"),
    ESTIMATION("Ước lượng");

    private final String vietnameseName;

    QuestionTag(String vietnameseName) {
        this.vietnameseName = vietnameseName;
    }



    /**
     * Get enum from Vietnamese name (case-insensitive)
     */
    public static QuestionTag fromVietnameseName(String vietnameseName) {
        if (vietnameseName == null) {
            return null;
        }
        for (QuestionTag tag : values()) {
            if (tag.vietnameseName.equalsIgnoreCase(vietnameseName.trim())) {
                return tag;
            }
        }
        return null;
    }
}
