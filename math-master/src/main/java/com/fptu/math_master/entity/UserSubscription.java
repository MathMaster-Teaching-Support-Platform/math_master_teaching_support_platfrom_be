package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fptu.math_master.enums.UserSubscriptionStatus;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "id",
    column = @Column(name = "subscription_id", updatable = false, nullable = false))
@Table(name = "user_subscriptions")
/**
 * The entity of 'UserSubscription' — records a user's subscription to a plan.
 */
public class UserSubscription extends BaseEntity {

  /**
   * user_id
   */
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /**
   * plan
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private SubscriptionPlan plan;

  /**
   * start_date
   */
  @Column(name = "start_date", nullable = false)
  private Instant startDate;

  /**
   * end_date — null for FOREVER plans
   */
  @Column(name = "end_date")
  private Instant endDate;

  /**
   * amount — price paid at time of subscription
   */
  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /**
   * currency
   */
  @Builder.Default
  @Column(name = "currency", nullable = false, length = 10)
  private String currency = "VND";

  /**
   * status
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserSubscriptionStatus status = UserSubscriptionStatus.ACTIVE;

  /**
   * payment_method — e.g. "payos", "momo", "manual"
   */
  @Column(name = "payment_method", length = 50)
  private String paymentMethod;

  /**
   * token_quota — tokens granted at purchase time.
   */
  @Builder.Default
  @Column(name = "token_quota", nullable = false)
  private Integer tokenQuota = 0;

  /**
   * token_remaining — remaining tokens user can spend on AI features.
   */
  @Builder.Default
  @Column(name = "token_remaining", nullable = false)
  private Integer tokenRemaining = 0;
}
