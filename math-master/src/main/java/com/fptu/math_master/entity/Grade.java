package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
    name = "grades",
    indexes = {
      @Index(name = "idx_grades_student", columnList = "student_id"),
      @Index(name = "idx_grades_lesson", columnList = "lesson_id"),
      @Index(name = "idx_grades_teacher", columnList = "teacher_id"),
      @Index(name = "idx_grades_student_lesson", columnList = "student_id, lesson_id")
    })
/**
 * The entity of 'Grade'.
 */
public class Grade extends BaseEntity {

  /**
   * student_id
   */
  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  /**
   * lesson_id
   */
  @Column(name = "lesson_id")
  private UUID lessonId;

  /**
   * assessment_id
   */
  @Column(name = "assessment_id")
  private UUID assessmentId;

  /**
   * teacher_id
   */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * score
   */
  @Column(name = "score", nullable = false, precision = 5, scale = 2)
  private BigDecimal score;

  /**
   * max_score
   */
  @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal maxScore;

  /**
   * percentage
   */
  @Column(name = "percentage", precision = 5, scale = 2, insertable = false, updatable = false)
  private BigDecimal percentage;

  /**
   * grade_letter
   */
  @Column(name = "grade_letter", length = 5)
  private String gradeLetter;

  /**
   * feedback
   */
  @Nationalized
  @Column(name = "feedback")
  private String feedback;

  /**
   * graded_at
   */
  @Column(name = "graded_at")
  private Instant gradedAt;

  /**
   * Relationships
   * - Many-to-One with User (student)
   * - Many-to-One with Lesson
   * - Many-to-One with Assessment
   * - Many-to-One with User (teacher)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (gradedAt == null) gradedAt = Instant.now();
  }
}
