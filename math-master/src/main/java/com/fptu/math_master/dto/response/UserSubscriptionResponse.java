package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fptu.math_master.enums.UserSubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponse {

  private UUID id;
  private UserInfo user;
  private PlanInfo plan;
  private Instant startDate;
  private Instant endDate;
  private BigDecimal amount;
  private String currency;
  private UserSubscriptionStatus status;
  private String paymentMethod;
  private Instant createdAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private UUID id;
    private String name;
    private String email;
    private String avatarInitial;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlanInfo {
    private UUID id;
    private String name;
    private String slug;
  }
}
