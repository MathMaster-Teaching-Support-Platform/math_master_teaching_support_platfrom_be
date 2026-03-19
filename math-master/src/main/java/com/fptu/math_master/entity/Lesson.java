package com.fptu.math_master.entity;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;

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
    name = "lessons",
    indexes = {
      @Index(name = "idx_lessons_chapter_id", columnList = "chapter_id"),
      @Index(name = "idx_lessons_status", columnList = "status")
    })
/**
 * The entity of 'Lesson'.
 */
public class Lesson extends BaseEntity {

  /**
   * chapter_id
   */
  @Column(name = "chapter_id", nullable = false)
  private UUID chapterId;

  /**
   * title
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  /**
   * learning_objectives
   */
  @Nationalized
  @Column(name = "learning_objectives")
  private String learningObjectives;

  /**
   * lesson_content
   */
  @Nationalized
  @Column(name = "lesson_content", nullable = false)
  private String lessonContent;

  /**
   * summary
   */
  @Nationalized
  @Column(name = "summary")
  private String summary;

  /**
   * order_index
   */
  @Column(name = "order_index")
  private Integer orderIndex;

  /**
   * duration_minutes
   */
  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  /**
   * difficulty
   */
  @Column(name = "difficulty")
  @Enumerated(EnumType.STRING)
  private LessonDifficulty difficulty;

  /**
   * status
   */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private LessonStatus status;

  /**
   * Relationships
   * - Many-to-One with Chapter
   * - One-to-Many with LessonPlan
   * - One-to-Many with QuestionTemplate
   * - One-to-Many with AssessmentLesson
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LessonPlan> lessonPlans;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuestionTemplate> questionTemplates;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentLesson> assessmentLessons;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (status == null) status = LessonStatus.DRAFT;
  }
}
