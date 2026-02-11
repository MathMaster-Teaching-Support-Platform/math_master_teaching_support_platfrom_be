package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "permissions",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
  }
)
public class Permission {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /**
   * Unique permission key used by code checks.
   * Example: "USER_READ", "USER_CREATE", "QUIZ_SUBMIT"
   */
  @NotBlank
  @Size(max = 100)
  @Column(name = "code", length = 100, nullable = false)
  private String code;

  /**
   * Human readable name.
   * Example: "Read user", "Create user"
   */
  @NotBlank
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Size(max = 500)
  @Nationalized
  @Column(name = "description", length = 500)
  private String description;
}
