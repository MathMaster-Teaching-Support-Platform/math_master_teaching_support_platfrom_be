package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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
      @Index(
          name = "idx_assessment_questions_matrix_mapping",
          columnList = "matrix_template_mapping_id"),
      @Index(name = "idx_assessment_questions_bank_mapping", columnList = "matrix_bank_mapping_id"),
      @Index(name = "idx_assessment_questions_order", columnList = "order_index")
    })
/**
 * The entity of 'AssessmentQuestion'.
 */
public class AssessmentQuestion extends BaseEntity {

  /**
   * assessment_id
   */
  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  /**
   * question_id
   */
  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  /**
   * order_index
   */
  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  /**
   * points_override
   */
  @Column(name = "points_override", precision = 5, scale = 2)
  private BigDecimal pointsOverride;

  /**
   * matrix_template_mapping_id
   */
  @Column(name = "matrix_template_mapping_id")
  private UUID matrixTemplateMappingId;

  @Column(name = "matrix_bank_mapping_id")
  private UUID matrixBankMappingId;

  /**
   * Relationships
   * - Many-to-One with Assessment
   * - Many-to-One with Question
   * - Many-to-One with ExamMatrixTemplateMapping
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_template_mapping_id", insertable = false, updatable = false)
  private ExamMatrixTemplateMapping examMatrixTemplateMapping;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_bank_mapping_id", insertable = false, updatable = false)
  private ExamMatrixBankMapping examMatrixBankMapping;
}
