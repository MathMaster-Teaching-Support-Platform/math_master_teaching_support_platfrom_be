package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "roles",
    uniqueConstraints = {@UniqueConstraint(name = "uk_role_name", columnNames = "name")})
/**
 * The entity of 'Role'.
 */
public class Role extends BaseEntity {

  /** Example: "ADMIN", "STUDENT" */
  @NotBlank
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @ManyToMany Set<Permission> permissions;
}
