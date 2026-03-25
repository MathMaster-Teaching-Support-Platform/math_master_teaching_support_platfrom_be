package com.fptu.math_master.entity;

import com.fptu.math_master.enums.DistributionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "point_distribution",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_point_distribution",
          columnNames = {"matrix_id", "distribution_type", "category_key", "category_value"})
    },
    indexes = {
      @Index(name = "idx_point_distribution_matrix", columnList = "matrix_id"),
      @Index(name = "idx_point_distribution_type", columnList = "distribution_type"),
      @Index(name = "idx_point_distribution_key", columnList = "category_key")
    })
/**
 * The entity of 'PointDistribution'.
 */
public class PointDistribution extends BaseEntity {

  /**
   * matrix_id
   */
  @Column(name = "matrix_id", nullable = false)
  private UUID matrixId;

  /**
   * distribution_type
   */
  @Column(name = "distribution_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private DistributionType distributionType;

  /**
   * category_key
   */
  @Size(max = 100)
  @Column(name = "category_key", length = 100, nullable = false)
  private String categoryKey;

  /**
   * category_value
   */
  @Size(max = 255)
  @Column(name = "category_value", length = 255, nullable = false)
  private String categoryValue;

  /**
   * num_questions
   */
  @Column(name = "num_questions", nullable = false)
  private Integer numQuestions;

  /**
   * total_points
   */
  @Column(name = "total_points", nullable = false, precision = 6, scale = 2)
  private BigDecimal totalPoints;

  /**
   * percentage
   */
  @Column(name = "percentage", precision = 5, scale = 2)
  private BigDecimal percentage;

  /**
   * Relationships
   * - Many-to-One with ExamMatrix
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (numQuestions == null) numQuestions = 0;
    if (totalPoints == null) totalPoints = BigDecimal.ZERO;
  }
}
