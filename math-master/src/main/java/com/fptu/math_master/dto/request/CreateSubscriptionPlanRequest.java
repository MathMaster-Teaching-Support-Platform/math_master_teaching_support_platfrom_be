package com.fptu.math_master.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.math_master.enums.BillingCycle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionPlanRequest {

  @NotBlank(message = "Plan name is required")
  private String name;

  private String description;

  /**
   * price — null is allowed for enterprise/contact-sales plans
   */
  @Min(value = 0, message = "Price must be >= 0")
  private BigDecimal price;

  @NotNull(message = "Billing cycle is required")
  private BillingCycle billingCycle;

  @NotEmpty(message = "At least one feature is required")
  private List<@NotBlank String> features;

  @Min(value = 0, message = "Token quota must be >= 0")
  @Builder.Default
  private Integer tokenQuota = 0;

  @Builder.Default
  private boolean featured = false;

  @JsonProperty("isPublic")
  @Builder.Default
  private boolean isPublic = true;
}
