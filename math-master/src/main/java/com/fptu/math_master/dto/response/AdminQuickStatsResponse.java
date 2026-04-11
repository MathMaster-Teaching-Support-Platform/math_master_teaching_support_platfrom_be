package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuickStatsResponse {

  private double conversionRate;
  private long activeUsers;
  private long documentsCreated;
  /**
   * satisfactionRate is not collected from user feedback yet.
   * Returns -1 when data is unavailable.
   */
  private double satisfactionRate;
}
