package com.fptu.math_master.entity;

import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.SubscriptionPlanStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "id",
    column = @Column(name = "plan_id", updatable = false, nullable = false))
@Table(name = "subscription_plans")
/**
 * The entity of 'SubscriptionPlan'.
 */
public class SubscriptionPlan extends BaseEntity {

  /**
   * name
   */
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /**
   * slug — URL-friendly unique identifier (e.g. "free", "teacher", "enterprise")
   */
  @Column(name = "slug", nullable = false, unique = true, length = 100)
  private String slug;

  /**
   * price — null means "contact sales" (enterprise)
   */
  @Column(name = "price", precision = 15, scale = 2)
  private BigDecimal price;

  /**
   * currency
   */
  @Builder.Default
  @Column(name = "currency", nullable = false, length = 10)
  private String currency = "VND";

  /**
   * billing_cycle
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_cycle", nullable = false)
  private BillingCycle billingCycle;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * is_featured — highlights the plan as "most popular"
   */
  @Builder.Default
  @Column(name = "is_featured", nullable = false)
  private boolean featured = false;

  /**
   * is_public — controls visibility on the public pricing page
   */
  @Builder.Default
  @Column(name = "is_public", nullable = false)
  private boolean publicVisible = true;

  /**
   * status
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionPlanStatus status = SubscriptionPlanStatus.ACTIVE;

  /**
   * features — stored as JSONB array
   */
  @Type(JsonBinaryType.class)
  @Column(name = "features", columnDefinition = "jsonb")
  private List<String> features;
}
