package com.fptu.math_master.entity;

import com.fptu.math_master.enums.QuestionDifficulty;
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
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * One explicit row in an exam-matrix table (ma trận đề thi).
 * <p>
 * A row represents a single <em>dạng bài</em> (question type / topic) sourced
 * from a specific Chapter and optionally a specific Lesson.  The associated
 * {@link ExamMatrixTemplateMapping} records are the <em>cells</em> of that row
 * — one cell per cognitive level (Nhận Biết, Thông Hiểu, Vận Dụng, VDC).
 * <p>
 * Matrix table layout:
 * <pre>
 * Lớp | Chương | Dạng bài             | Ref | NB | TH | VD | VDC | Total/row
 * 12  | Đạo hàm| Đơn điệu của HS      | 3,30|  1 |  1 |    |     |   2
 * 12  | Đạo hàm| Cực trị của HS       |4,5,…|  1 |  1 |  1 |  1  |   4
 * </pre>
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
  @Deprecated
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "exam_matrix_rows",
    indexes = {
      @Index(name = "idx_emr_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_emr_chapter", columnList = "chapter_id"),
      @Index(name = "idx_emr_lesson", columnList = "lesson_id"),
      @Index(name = "idx_emr_subject", columnList = "subject_id"),
      @Index(name = "idx_emr_difficulty", columnList = "question_difficulty"),
      @Index(name = "idx_emr_order", columnList = "order_index")
    })
public class ExamMatrixRow extends BaseEntity {

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  /** Which chapter (chương) this row belongs to. */
  @Column(name = "chapter_id")
  private UUID chapterId;

  /** Optional: narrow to a specific lesson (bài) within the chapter. */
  @Column(name = "lesson_id")
  private UUID lessonId;

  /** Snapshot of subject id at row creation time. */
  @Column(name = "subject_id")
  private UUID subjectId;

  /** Snapshot of subject name at row creation time. */
  @Size(max = 255)
  @Nationalized
  @Column(name = "subject_name", length = 255)
  private String subjectName;

  /** Snapshot of school-grade name (lop) at row creation time. */
  @Size(max = 100)
  @Nationalized
  @Column(name = "school_grade_name", length = 100)
  private String schoolGradeName;

  /** Snapshot of chapter title at row creation time. */
  @Size(max = 255)
  @Nationalized
  @Column(name = "chapter_name", length = 255)
  private String chapterName;

  /** Selected row-level difficulty used by bank mappings for this row. */
  @Column(name = "question_difficulty")
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty questionDifficulty;

  /**
   * Human-readable name of the question type / dạng bài shown in the matrix,
   * e.g. "Đơn điệu của HS", "PT Mũ – Logarit".
   */
  @Size(max = 500)
  @Nationalized
  @Column(name = "question_type_name", length = 500)
  private String questionTypeName;

  /**
   * Reference question numbers from the illustrative exam paper, e.g. "3,30".
   * Purely informational — matches the "Trích dẫn đề Minh Họa" column.
   */
  @Size(max = 255)
  @Column(name = "reference_questions", length = 255)
  private String referenceQuestions;

  /** Ordering within the chapter group (1-based). */
  @Column(name = "order_index")
  private Integer orderIndex;

  // ── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  /** The individual cognitive-level cells for this row. */
  @OneToMany(mappedBy = "matrixRow", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> cells;

  /** The bank-based cells for this row (new matrix system). */
  @OneToMany(mappedBy = "matrixRow", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixBankMapping> bankMappings;

  // ── Lifecycle hooks ──────────────────────────────────────────────────────

}
