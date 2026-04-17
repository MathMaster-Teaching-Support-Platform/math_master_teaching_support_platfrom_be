package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionStatsResponse {

  private long total;
  private long completed;
  private long pending;
  private long failed;
  /** Sum of amount for all SUCCESS transactions (VND) */
  private long totalRevenue;
}
