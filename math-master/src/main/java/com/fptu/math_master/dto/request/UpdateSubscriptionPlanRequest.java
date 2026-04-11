package com.fptu.math_master.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.SubscriptionPlanStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionPlanRequest {

  @NotBlank(message = "Plan name must not be blank")
  private String name;

  private String description;

  /**
   * price — null = contact-sales; 0 = free
   */
  @Min(value = 0, message = "Price must be >= 0")
  private BigDecimal price;

  private BillingCycle billingCycle;

  private List<@NotBlank String> features;

  private Boolean featured;

  @JsonProperty("isPublic")
  private Boolean isPublic;

  private SubscriptionPlanStatus status;
}
