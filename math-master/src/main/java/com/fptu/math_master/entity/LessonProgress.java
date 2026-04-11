package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
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
    name = "lesson_progress",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_lesson_progress_enrollment_lesson",
          columnNames = {"enrollment_id", "course_lesson_id"})
    },
    indexes = {
      @Index(name = "idx_lesson_progress_enrollment_id", columnList = "enrollment_id"),
      @Index(
          name = "idx_lesson_progress_enrollment_lesson",
          columnList = "enrollment_id, course_lesson_id")
    })
public class LessonProgress extends BaseEntity {

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "course_lesson_id", nullable = false)
  private UUID courseLessonId;

  @Column(name = "is_completed", nullable = false)
  @Builder.Default
  private boolean isCompleted = false;

  @Column(name = "completed_at")
  private Instant completedAt;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_lesson_id", insertable = false, updatable = false)
  private CourseLesson courseLesson;
}
