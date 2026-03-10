package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "curricula",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_curricula_name_grade_category",
          columnNames = {"name", "grade", "category"})
    },
    indexes = {
      @Index(name = "idx_curricula_grade", columnList = "grade"),
      @Index(name = "idx_curricula_category", columnList = "category"),
      @Index(name = "idx_curricula_grade_category", columnList = "grade, category")
    })
public class  Curriculum {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Min(1)
  @Max(12)
  @Column(name = "grade", nullable = false)
  private Integer grade;

  @Column(name = "category", nullable = false)
  @Enumerated(EnumType.STRING)
  private CurriculumCategory category;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @OneToMany(mappedBy = "curriculum", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Chapter> chapters;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
