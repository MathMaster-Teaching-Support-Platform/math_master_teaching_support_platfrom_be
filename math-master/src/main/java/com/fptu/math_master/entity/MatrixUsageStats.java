package com.fptu.math_master.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "matrix_usage_stats",
    indexes = {
      @Index(name = "idx_matrix_usage_stats_matrix", columnList = "matrix_id"),
      @Index(name = "idx_matrix_usage_stats_assessment", columnList = "assessment_id")
    })
/**
 * The entity of 'MatrixUsageStats'.
 */
public class MatrixUsageStats extends BaseEntity {

  /**
   * matrix_id
   */
  @Column(name = "matrix_id", nullable = false)
  private UUID matrixId;

  /**
   * assessment_id
   */
  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  /**
   * students_completed
   */
  @Column(name = "students_completed")
  private Integer studentsCompleted;

  /**
   * avg_score
   */
  @Column(name = "avg_score", precision = 5, scale = 2)
  private BigDecimal avgScore;

  /**
   * difficulty_accuracy
   */
  @Type(JsonBinaryType.class)
  @Column(name = "difficulty_accuracy", columnDefinition = "jsonb")
  private Map<String, Object> difficultyAccuracy;

  /**
   * Relationships
   * - Many-to-One with ExamMatrix
   * - Many-to-One with Assessment
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @PrePersist
  public void prePersist() {
    if (studentsCompleted == null) studentsCompleted = 0;
  }
}
