package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "assessment_questions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_assessment_questions",
          columnNames = {"assessment_id", "question_id"}),
      @UniqueConstraint(
          name = "uq_assessment_questions_order",
          columnNames = {"assessment_id", "order_index"})
    },
    indexes = {
      @Index(name = "idx_assessment_questions_assessment", columnList = "assessment_id"),
      @Index(name = "idx_assessment_questions_question", columnList = "question_id"),
      @Index(name = "idx_assessment_questions_matrix_mapping", columnList = "matrix_template_mapping_id"),
      @Index(name = "idx_assessment_questions_order", columnList = "order_index")
    })
public class AssessmentQuestion {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @Column(name = "points_override", precision = 5, scale = 2)
  private BigDecimal pointsOverride;

  @Column(name = "matrix_template_mapping_id")
  private UUID matrixTemplateMappingId;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_template_mapping_id", insertable = false, updatable = false)
  private ExamMatrixTemplateMapping examMatrixTemplateMapping;

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
