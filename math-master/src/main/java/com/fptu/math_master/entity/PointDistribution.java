package com.fptu.math_master.entity;

import com.fptu.math_master.enums.DistributionType;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "point_distribution",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_point_distribution", columnNames = {"matrix_id", "distribution_type", "category_key", "category_value"})
  },
  indexes = {
    @Index(name = "idx_point_distribution_matrix", columnList = "matrix_id"),
    @Index(name = "idx_point_distribution_type", columnList = "distribution_type"),
    @Index(name = "idx_point_distribution_key", columnList = "category_key")
  }
)
public class PointDistribution {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "matrix_id", nullable = false)
  private UUID matrixId;

  @Column(name = "distribution_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private DistributionType distributionType;

  @Size(max = 100)
  @Column(name = "category_key", length = 100, nullable = false)
  private String categoryKey;

  @Size(max = 255)
  @Column(name = "category_value", length = 255, nullable = false)
  private String categoryValue;

  @Column(name = "num_questions", nullable = false)
  private Integer numQuestions;

  @Column(name = "total_points", nullable = false, precision = 6, scale = 2)
  private BigDecimal totalPoints;

  @Column(name = "percentage", precision = 5, scale = 2)
  private BigDecimal percentage;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @PrePersist
  public void prePersist() {
    if (numQuestions == null) numQuestions = 0;
    if (totalPoints == null) totalPoints = BigDecimal.ZERO;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

