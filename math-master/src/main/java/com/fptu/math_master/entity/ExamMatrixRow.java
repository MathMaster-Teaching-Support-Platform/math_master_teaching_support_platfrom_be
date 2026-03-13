package com.fptu.math_master.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.util.UuidV7Generator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Entity
@Table(
    name = "exam_matrix_rows",
    indexes = {
      @Index(name = "idx_emr_matrix",  columnList = "exam_matrix_id"),
      @Index(name = "idx_emr_chapter", columnList = "chapter_id"),
      @Index(name = "idx_emr_lesson",  columnList = "lesson_id"),
      @Index(name = "idx_emr_tpl",     columnList = "template_id"),
      @Index(name = "idx_emr_order",   columnList = "order_index")
    })
public class ExamMatrixRow {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  /** Which chapter (chương) this row belongs to. */
  @Column(name = "chapter_id")
  private UUID chapterId;

  /** Optional: narrow to a specific lesson (bài) within the chapter. */
  @Column(name = "lesson_id")
  private UUID lessonId;

  /**
   * If this row is backed by an existing {@link QuestionTemplate}, store its id.
   * The template's name will be used as {@code questionTypeName} by default.
   */
  @Column(name = "template_id")
  private UUID templateId;

  /**
   * Human-readable name of the question type / dạng bài shown in the matrix,
   * e.g. "Đơn điệu của HS", "PT Mũ – Logarit".
   * Required when {@code templateId} is null.
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

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", insertable = false, updatable = false)
  private QuestionTemplate questionTemplate;

  /** The individual cognitive-level cells for this row. */
  @OneToMany(mappedBy = "matrixRow", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> cells;

  // ── Lifecycle hooks ──────────────────────────────────────────────────────

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
