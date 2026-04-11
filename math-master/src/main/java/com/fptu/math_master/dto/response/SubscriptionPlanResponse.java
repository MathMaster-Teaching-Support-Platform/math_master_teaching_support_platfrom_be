package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.SubscriptionPlanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {

  private UUID id;
  private String name;
  private String slug;

  /**
   * null for enterprise/contact-sales plans
   */
  private BigDecimal price;

  private String currency;
  private BillingCycle billingCycle;
  private String description;
  private boolean featured;

  @JsonProperty("isPublic")
  private boolean isPublic;
  private SubscriptionPlanStatus status;
  private List<String> features;
  private PlanStats stats;
  private Instant createdAt;
  private Instant updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlanStats {
    private long activeUsers;
    private long revenueThisMonth;
    private double growthPercent;
  }
}
