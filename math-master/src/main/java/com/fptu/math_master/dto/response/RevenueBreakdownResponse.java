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
public class RevenueBreakdownResponse {
    private String period; // "7d", "30d", "90d", "1y"
    private List<DailyRevenue> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private String date; // ISO date string
        private BigDecimal deposits;
        private BigDecimal subscriptions;
        private BigDecimal courseSales; // platform commission only
        private BigDecimal total;
    }
}
