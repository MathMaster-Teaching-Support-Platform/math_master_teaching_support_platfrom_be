package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
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
@Table(name = "token_cost_audit_logs")
public class TokenCostAuditLog extends BaseEntity {

  @Column(name = "admin_id", nullable = false)
  private UUID adminId;

  @Column(name = "admin_name")
  private String adminName;

  @Column(name = "feature_key", nullable = false)
  private String featureKey;

  @Column(name = "old_value")
  private String oldValue;

  @Column(name = "new_value")
  private String newValue;

  @Column(name = "change_type")
  private String changeType; // e.g., "COST_UPDATE", "STATUS_TOGGLE"
}
