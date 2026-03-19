package com.fptu.math_master.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(
    name = "permissions",
    uniqueConstraints = {@UniqueConstraint(name = "uk_permission_code", columnNames = "code")})
/**
 * The entity of 'Permission'.
 */
public class Permission extends BaseEntity {

  /**
   * Unique permission key used by code checks. Example: "USER_READ", "USER_CREATE", "QUIZ_SUBMIT"
   */
  @NotBlank
  @Size(max = 100)
  @Column(name = "code", length = 100, nullable = false)
  private String code;

  /** Human readable name. Example: "Read user", "Create user" */
  @NotBlank
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;
}
