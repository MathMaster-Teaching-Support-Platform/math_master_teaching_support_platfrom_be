package com.fptu.math_master.entity;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

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
@Data
@Entity
@Table(
    name = "roadmap_topics",
    indexes = {
      @Index(name = "idx_roadmap_topics_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_roadmap_topics_lesson", columnList = "lesson_id"),
      @Index(name = "idx_roadmap_topics_status", columnList = "status"),
      @Index(name = "idx_roadmap_topics_sequence", columnList = "sequence_order")
    })
public class RoadmapTopic {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  @Column(name = "lesson_id")
  private UUID lessonId; // Reference to lesson entity

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title; // Topic name

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TopicStatus status; // NOT_STARTED, IN_PROGRESS, COMPLETED, LOCKED

  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty; // EASY, MEDIUM, HARD

  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder; // Order in which topics should be learned (1, 2, 3...)

  @Column(name = "priority", nullable = false)
  private Integer priority = 0;  // Higher = more important (negative for weak areas = negative numbers)

  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage; // 0-100%

  @Column(name = "estimated_hours", nullable = false)
  private Integer estimatedHours = 1; // Estimated time to complete

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @PrePersist
  public void prePersist() {
    if (status == null) status = TopicStatus.NOT_STARTED;
    if (progressPercentage == null) progressPercentage = BigDecimal.ZERO;
    if (estimatedHours == null) estimatedHours = 1;
    if (priority == null) priority = 0;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
