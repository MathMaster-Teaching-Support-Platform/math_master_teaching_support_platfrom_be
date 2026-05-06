package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "token_cost_config")
public class TokenCostConfig extends BaseEntity {

  @Column(name = "feature_key", unique = true, nullable = false)
  private String featureKey;

  @Column(name = "feature_label", nullable = false)
  private String featureLabel;

  @Column(name = "cost_per_use", nullable = false)
  private Integer costPerUse;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;
}
