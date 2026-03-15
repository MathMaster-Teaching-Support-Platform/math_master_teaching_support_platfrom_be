package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "assessment_lessons",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_assessment_lessons_assessment_lesson",
          columnNames = {"assessment_id", "lesson_id"})
    },
    indexes = {
      @Index(name = "idx_assessment_lessons_assessment", columnList = "assessment_id"),
      @Index(name = "idx_assessment_lessons_lesson", columnList = "lesson_id")
    })
/**
 * The entity of 'AssessmentLesson'.
 */
public class AssessmentLesson extends BaseEntity {

  /**
   * assessment_id
   */
  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  /**
   * lesson_id
   */
  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  /**
   * Relationships
   * - Many-to-One with Assessment
   * - Many-to-One with Lesson
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;
}
