package com.fptu.math_master.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowSummaryResponse {

    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private BigDecimal netCashFlow;

    /** Trend vs previous period (percentage, can be null if no previous data). */
    private Double inflowTrend;
    private Double outflowTrend;
    private Double netTrend;

    private String fromDate;
    private String toDate;
    private String period;

    /** Category breakdown for donut chart. */
    private List<CategoryBreakdownItem> categoryBreakdown;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryBreakdownItem {
        private String categoryName;
        private String color;
        private String type;
        private BigDecimal total;
        private double percentage;
    }
}
