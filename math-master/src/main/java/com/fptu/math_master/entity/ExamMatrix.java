package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.MatrixStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "exam_matrices",
    indexes = {
      @Index(name = "idx_exam_matrices_teacher", columnList = "teacher_id"),
      @Index(name = "idx_exam_matrices_status", columnList = "status"),
      @Index(name = "idx_exam_matrices_is_reusable", columnList = "is_reusable"),
      @Index(name = "idx_exam_matrices_grade", columnList = "grade_level"),
      @Index(name = "idx_exam_matrices_bank", columnList = "question_bank_id")
    })
/**
 * The entity of 'ExamMatrix'.
 */
public class ExamMatrix extends BaseEntity {

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * Cache of the target school-grade level (lớp) for this matrix (e.g. 10, 11, 12).
   * A matrix may reference content from multiple grades; this is the primary grade.
   */
  @Column(name = "grade_level")
  private Integer gradeLevel;

  /**
   * Cache of the subject ID for this matrix.
   * Auto-populated from the first question bank added to the matrix.
   */
  @Column(name = "subject_id")
  private UUID subjectId;

  /**
   * The single question bank used as the source for all questions in this matrix.
   * In the new chapter-based architecture, one matrix maps to exactly one bank.
   * Questions are filtered by chapter within that bank.
   */
  @Column(name = "question_bank_id", nullable = false)
  private UUID questionBankId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_reusable", nullable = false)
  private Boolean isReusable;

  @Column(name = "total_questions_target")
  private Integer totalQuestionsTarget;

  @Column(name = "total_points_target", precision = 6, scale = 2)
  private BigDecimal totalPointsTarget;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MatrixStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_bank_id", insertable = false, updatable = false)
  private QuestionBank questionBank;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> templateMappings;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixBankMapping> bankMappings;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixRow> matrixRows;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (isReusable == null) isReusable = false;
    if (status == null) status = MatrixStatus.DRAFT;
  }
}
