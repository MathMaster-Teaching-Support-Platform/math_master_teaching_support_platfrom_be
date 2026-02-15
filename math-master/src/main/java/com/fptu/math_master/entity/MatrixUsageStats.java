package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "matrix_usage_stats",
    indexes = {
      @Index(name = "idx_matrix_usage_stats_matrix", columnList = "matrix_id"),
      @Index(name = "idx_matrix_usage_stats_assessment", columnList = "assessment_id")
    })
public class MatrixUsageStats {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "matrix_id", nullable = false)
  private UUID matrixId;

  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  @Column(name = "students_completed")
  private Integer studentsCompleted;

  @Column(name = "avg_score", precision = 5, scale = 2)
  private BigDecimal avgScore;

  @Type(JsonBinaryType.class)
  @Column(name = "difficulty_accuracy", columnDefinition = "jsonb")
  private Map<String, Object> difficultyAccuracy;

  @Column(name = "created_at")
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @PrePersist
  public void prePersist() {
    if (studentsCompleted == null) studentsCompleted = 0;
    if (createdAt == null) createdAt = Instant.now();
  }
}
