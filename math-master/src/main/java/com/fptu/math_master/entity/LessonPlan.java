package com.fptu.math_master.entity;

import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "lesson_plans",
    indexes = {
      @Index(name = "idx_lesson_plans_lesson_id", columnList = "lesson_id"),
      @Index(name = "idx_lesson_plans_teacher_id", columnList = "teacher_id")
    })
/**
 * The entity of 'LessonPlan'.
 */
public class LessonPlan extends BaseEntity {

  /**
   * lesson_id
   */
  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  /**
   * teacher_id
   */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * objectives
   */
  @Type(StringArrayType.class)
  @Column(name = "objectives", columnDefinition = "TEXT[]")
  private String[] objectives;

  /**
   * materials_needed
   */
  @Type(StringArrayType.class)
  @Column(name = "materials_needed", columnDefinition = "TEXT[]")
  private String[] materialsNeeded;

  /**
   * teaching_strategy
   */
  @Nationalized
  @Column(name = "teaching_strategy")
  private String teachingStrategy;

  /**
   * assessment_methods
   */
  @Nationalized
  @Column(name = "assessment_methods")
  private String assessmentMethods;

  /**
   * notes
   */
  @Nationalized
  @Column(name = "notes")
  private String notes;

  /**
   * Relationships
   * - Many-to-One with Lesson
   * - Many-to-One with User (teacher)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;
}
