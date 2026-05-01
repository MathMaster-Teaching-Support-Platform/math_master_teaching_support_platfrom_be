package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "exam_matrix_bank_mappings",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_exam_matrix_row_part_cognitive",
          columnNames = {"exam_matrix_id", "matrix_row_id", "part_number", "cognitive_level"})
    },
    indexes = {
      @Index(name = "idx_exam_matrix_bank_mapping_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_exam_matrix_bank_mapping_row", columnList = "matrix_row_id"),
      @Index(name = "idx_exam_matrix_bank_mapping_cognitive", columnList = "cognitive_level")
    })
public class ExamMatrixBankMapping extends BaseEntity {

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  @Column(name = "matrix_row_id", nullable = false)
  private UUID matrixRowId;

  @Column(name = "question_count", nullable = false)
  private Integer questionCount;

  @Column(name = "points_per_question", precision = 7, scale = 2)
  private BigDecimal pointsPerQuestion;

  // Nullable for percentage-based matrices (cognitive level determined by matrix-level percentages)
  @Column(name = "cognitive_level")
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  /**
   * The exam part number (1, 2, or 3) that this cell belongs to.
   * Part type is defined by exam_matrix_parts table, not hardcoded.
   * Default: 1 for backward compatibility
   */
  @Column(name = "part_number", nullable = false)
  @Builder.Default
  private Integer partNumber = 1;

  /**
   * FK to exam_matrix_parts table.
   * Defines the question type for this cell dynamically.
   * Nullable for legacy data migration.
   */
  @Column(name = "part_id")
  private UUID partId;

  /**
   * The type of question this cell requires (MCQ, TRUE_FALSE, or SHORT_ANSWER).
   * Denormalized from exam_matrix_parts for query performance.
   * Default: MULTIPLE_CHOICE for backward compatibility
   */
  @Column(name = "question_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_row_id", insertable = false, updatable = false)
  private ExamMatrixRow matrixRow;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "part_id", insertable = false, updatable = false)
  private ExamMatrixPart examMatrixPart;

  @OneToMany(mappedBy = "examMatrixBankMapping", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;
}
