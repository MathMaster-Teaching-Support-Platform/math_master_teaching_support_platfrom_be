package com.fptu.math_master.dto.response;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private long totalUsers;
  private double totalUsersGrowthPercent;

  private long monthlyRevenue;
  private double monthlyRevenueGrowthPercent;

  private long activeEnrollments;
  private double activeEnrollmentsGrowthPercent;

  private long totalTransactions;
  private double totalTransactionsGrowthPercent;

  private String month;
}
