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
          name = "uq_exam_matrix_row_bank_cognitive",
          columnNames = {"exam_matrix_id", "matrix_row_id", "question_bank_id", "cognitive_level"})
    },
    indexes = {
      @Index(name = "idx_exam_matrix_bank_mapping_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_exam_matrix_bank_mapping_bank", columnList = "question_bank_id"),
      @Index(name = "idx_exam_matrix_bank_mapping_cognitive", columnList = "cognitive_level")
    })
public class ExamMatrixBankMapping extends BaseEntity {

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  @Column(name = "question_bank_id", nullable = false)
  private UUID questionBankId;

  @Column(name = "matrix_row_id")
  private UUID matrixRowId;

  @Column(name = "question_count", nullable = false)
  private Integer questionCount;

  @Column(name = "points_per_question", precision = 7, scale = 2)
  private BigDecimal pointsPerQuestion;

  // Nullable for percentage-based matrices (cognitive level determined by matrix-level percentages)
  @Column(name = "cognitive_level")
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_bank_id", insertable = false, updatable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_row_id", insertable = false, updatable = false)
  private ExamMatrixRow matrixRow;

  @OneToMany(mappedBy = "examMatrixBankMapping", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;
}
