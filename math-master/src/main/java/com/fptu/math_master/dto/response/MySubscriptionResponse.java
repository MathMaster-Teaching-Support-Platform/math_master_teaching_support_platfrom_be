package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySubscriptionResponse {

  private UUID subscriptionId;
  private UUID planId;
  private String planName;
  private String planSlug;
  private BillingCycle billingCycle;
  private UserSubscriptionStatus status;
  private Instant startDate;
  private Instant endDate;
  private BigDecimal amount;
  private String currency;
  private Integer tokenQuota;
  private Integer tokenRemaining;
  private String paymentMethod;
}
