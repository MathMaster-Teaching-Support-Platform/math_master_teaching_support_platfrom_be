package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

/**
 * Represents a topic/module within a learning roadmap
 *
 * <p>Topics are organized by:
 * - Lesson (from the curriculum)
 * - Difficulty level
 * - Status (not started, in progress, completed, locked)
 * - Progress tracking
 *
 * <p>Topics have assessments and mindmaps organized by lessons.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "roadmap_topics",
    indexes = {
      @Index(name = "idx_roadmap_topics_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_roadmap_topics_lesson", columnList = "lesson_id"),
      @Index(name = "idx_roadmap_topics_status", columnList = "status"),
      @Index(name = "idx_roadmap_topics_sequence", columnList = "sequence_order")
    })
public class RoadmapTopic extends BaseEntity {

  /**
   * roadmap_id
   */
  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  /**
   * lesson_id
   */
  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "topic_assessment_id")
  private UUID topicAssessmentId;

  /**
   * title
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * status
   */
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TopicStatus status;

  /**
   * difficulty
   */
  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty;

  /**
   * sequence_order
   */
  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder;

  /**
   * priority
   */
  @Builder.Default
  @Column(name = "priority", nullable = false)
  private Integer priority = 0;

  /**
   * progress_percentage
   */
  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage;

  @Column(name = "pass_threshold_percentage", precision = 5, scale = 2)
  private BigDecimal passThresholdPercentage;

  /**
   * estimated_hours
   */
  @Builder.Default
  @Column(name = "estimated_hours", nullable = false)
  private Integer estimatedHours = 1;

  /**
   * mark
   *
   * <p>Expected checkpoint score on a 10-point scale used to determine student level placement.
   */
  @Column(name = "mark")
  private Double mark;

  /**
   * started_at
   */
  @Column(name = "started_at")
  private Instant startedAt;

  /**
   * completed_at
   */
  @Column(name = "completed_at")
  private Instant completedAt;

  /**
   * Relationships
   * - Many-to-One with LearningRoadmap
   * - Many-to-One with Lesson
   */
  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "topic_assessment_id", insertable = false, updatable = false)
  private Assessment topicAssessment;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "roadmap_topic_teaching_resources",
      joinColumns = @JoinColumn(name = "roadmap_topic_id"),
      inverseJoinColumns = @JoinColumn(name = "teaching_resource_id"))
  private Set<TeachingResource> teachingResources;

  @PrePersist
  @Override
  public void prePersist() {
    super.prePersist();
    if (status == null) status = TopicStatus.NOT_STARTED;
    if (progressPercentage == null) progressPercentage = BigDecimal.ZERO;
    if (passThresholdPercentage == null) passThresholdPercentage = BigDecimal.valueOf(70);
    if (estimatedHours == null) estimatedHours = 1;
    if (priority == null) priority = 0;
  }
}
