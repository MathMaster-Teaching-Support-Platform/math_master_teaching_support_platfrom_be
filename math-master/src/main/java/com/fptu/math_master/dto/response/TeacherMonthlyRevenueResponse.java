package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherMonthlyRevenueResponse {
    private int year;
    private List<MonthRevenue> months;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthRevenue {
        private int month; // 1-12
        private String monthName; // "Jan", "Feb", etc.
        private BigDecimal revenue;
    }
}
