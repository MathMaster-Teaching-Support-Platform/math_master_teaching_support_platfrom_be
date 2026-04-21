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

/**
 * Represents a topic/module within a learning roadmap.
 *
 * Students can click any topic freely — there is no locking.
 * Topics are ordered visually (sequenceOrder) for guidance only.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
    name = "roadmap_topics",
    indexes = {
      @Index(name = "idx_roadmap_topics_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_roadmap_topics_status", columnList = "status"),
      @Index(name = "idx_roadmap_topics_sequence", columnList = "sequence_order")
    })
public class RoadmapTopic extends BaseEntity {

  @EqualsAndHashCode.Include
  private UUID entityIdForEquality() {
    return getId();
  }

  /** roadmap_id — the parent roadmap */
  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  /** title */
  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  /** description */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** status — used for optional non-blocking progress tracking */
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TopicStatus status;

  /** difficulty */
  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty;

  /** sequence_order — visual ordering, NOT a lock gate */
  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder;

  /** priority */
  @Builder.Default
  @Column(name = "priority", nullable = false)
  private Integer priority = 0;

  /** progress_percentage — optional non-blocking progress */
  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage;

  @Column(name = "pass_threshold_percentage", precision = 5, scale = 2)
  private BigDecimal passThresholdPercentage;

  /** estimated_hours */
  @Builder.Default
  @Column(name = "estimated_hours", nullable = false)
  private Integer estimatedHours = 1;

  /**
   * mark — entry test score checkpoint used to SUGGEST which topic to start from (non-blocking).
   * Students can still access any topic regardless of this value.
   */
  @Column(name = "mark")
  private Double mark;

  /** started_at */
  @Column(name = "started_at")
  private Instant startedAt;

  /** completed_at */
  @Column(name = "completed_at")
  private Instant completedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

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
