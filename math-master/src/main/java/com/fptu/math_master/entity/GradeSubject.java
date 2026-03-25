package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.*;

/**
 * N-N join entity between a school grade level (lớp) and a {@link Subject}.
 * <p>
 * Example: Grade 12 ↔ Subjects: Đại Số, Hình Học, Giải Tích, Tổ Hợp-XS.
 * <p>
 * This drives which subjects appear for a given grade when a teacher builds
 * an exam matrix — the hierarchy is:
 * <pre>
 *   GradeSubject (lớp × môn)
 *       └── Curriculum (chương trình cụ thể)
 *               └── Chapter (chương)
 *                       └── Lesson (bài)
 *                               └── QuestionTemplate (dạng bài)
 * </pre>
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "grade_subjects",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_grade_subjects_level_subject",
          columnNames = {"grade_level", "subject_id"})
    },
    indexes = {
      @Index(name = "idx_grade_subjects_level", columnList = "grade_level"),
      @Index(name = "idx_grade_subjects_subject", columnList = "subject_id"),
      @Index(name = "idx_grade_subjects_active", columnList = "is_active")
    })
public class GradeSubject extends BaseEntity {

  /** School grade level, e.g. 10, 11, 12. */
  @Min(1)
  @Max(12)
  @Column(name = "grade_level", nullable = false)
  private Integer gradeLevel;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  // ── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subject;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (isActive == null) isActive = true;
  }
}
