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
    name = "course_assessments",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_course_assessments_course_assessment",
          columnNames = {"course_id", "assessment_id"})
    },
    indexes = {
      @Index(name = "idx_course_assessments_course_id", columnList = "course_id"),
      @Index(name = "idx_course_assessments_assessment_id", columnList = "assessment_id"),
      @Index(name = "idx_course_assessments_order_index", columnList = "order_index"),
      @Index(name = "idx_course_assessments_deleted_at", columnList = "deleted_at")
    })
/**
 * Join entity linking Course with Assessment for course curriculum management.
 * Allows teachers to assign assessments to courses with ordering and requirement flags.
 */
public class CourseAssessment extends BaseEntity {

  /**
   * course_id - Foreign key to courses table
   */
  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  /**
   * assessment_id - Foreign key to assessments table
   */
  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  /**
   * order_index - Display order of assessment within the course
   */
  @Column(name = "order_index")
  private Integer orderIndex;

  /**
   * is_required - Whether students must complete this assessment
   */
  @Column(name = "is_required", nullable = false)
  @Builder.Default
  private boolean isRequired = true;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;
}
