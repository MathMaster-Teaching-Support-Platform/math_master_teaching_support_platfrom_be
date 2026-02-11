package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "roles",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_name", columnNames = "name")
  }
)
public class Role {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /**
   * Example: "ADMIN", "STUDENT"
   */
  @NotBlank
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @ManyToMany
  Set<Permission> permissions;
}
