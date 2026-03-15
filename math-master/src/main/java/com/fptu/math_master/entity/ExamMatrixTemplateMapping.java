package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Set;
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
    name = "exam_matrix_template_mappings",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_exam_matrix_template",
          columnNames = {"exam_matrix_id", "template_id"})
    },
    indexes = {
      @Index(name = "idx_exam_matrix_template_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_exam_matrix_template_template", columnList = "template_id"),
      @Index(name = "idx_exam_matrix_template_cognitive", columnList = "cognitive_level"),
      @Index(name = "idx_exam_matrix_template_row", columnList = "matrix_row_id")
    })
/**
 * The entity of 'ExamMatrixTemplateMapping'.
 */
public class ExamMatrixTemplateMapping extends BaseEntity {

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  /**
   * Back-reference to the {@link ExamMatrixRow} this cell belongs to.
   * Null for legacy mappings created without the structured builder.
   */
  @Column(name = "matrix_row_id")
  private UUID matrixRowId;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Column(name = "question_count", nullable = false)
  private Integer questionCount;

  @Column(name = "points_per_question", nullable = false, precision = 5, scale = 2)
  private BigDecimal pointsPerQuestion;

  @Column(name = "total_points", precision = 6, scale = 2, insertable = false, updatable = false)
  private BigDecimal totalPoints;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", insertable = false, updatable = false)
  private QuestionTemplate questionTemplate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_row_id", insertable = false, updatable = false)
  private ExamMatrixRow matrixRow;

  @OneToMany(
      mappedBy = "examMatrixTemplateMapping",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;
}
