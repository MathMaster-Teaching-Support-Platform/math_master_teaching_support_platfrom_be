package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFinancialOverviewResponse {
    private BigDecimal totalRevenue;
    private double totalRevenueTrend;
    private BigDecimal platformCommission;
    private double platformCommissionTrend;
    private long activeSubscriptions;
    private double activeSubscriptionsTrend;
    private long totalInstructors;
    private double totalInstructorsTrend;
    private BigDecimal avgOrderValue;
    private double avgOrderValueTrend;
    private long activeUsers;
    private double activeUsersTrend;
    private double conversionRate;
    private double conversionRateTrend;
    private double churnRate;
    private double churnRateTrend;
    private String period; // e.g., "2026-04"
}
