package com.fptu.math_master.entity;

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
 * Represents a subtopic within a roadmap topic
 *
 * <p>Subtopics provide granular breakdown of learning:
 * - Parent topic
 * - Specific learning objectives
 * - Related questions and materials
 * - Progress tracking
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "roadmap_subtopics",
    indexes = {
      @Index(name = "idx_subtopics_topic", columnList = "topic_id"),
      @Index(name = "idx_subtopics_status", columnList = "status"),
      @Index(name = "idx_subtopics_sequence", columnList = "sequence_order")
    })
public class RoadmapSubtopic {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "topic_id", nullable = false)
  private UUID topicId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TopicStatus status;

  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder;

  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage;

  @Column(name = "estimated_minutes", nullable = false)
  private Integer estimatedMinutes = 30;

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
  @JoinColumn(name = "topic_id", insertable = false, updatable = false)
  private RoadmapTopic topic;

  @PrePersist
  public void prePersist() {
    if (status == null) status = TopicStatus.NOT_STARTED;
    if (progressPercentage == null) progressPercentage = BigDecimal.ZERO;
    if (estimatedMinutes == null) estimatedMinutes = 30;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
