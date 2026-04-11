package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionOverallStatsResponse {

  private long totalRevenue;
  private double totalRevenueTrend;

  private long totalPaidUsers;
  private double totalPaidUsersTrend;

  private long avgRevenuePerUser;
  private double avgRevenuePerUserTrend;

  private double conversionRate;
  private double conversionRateTrend;

  /**
   * period — format YYYY-MM, e.g. "2026-04"
   */
  private String period;
}
