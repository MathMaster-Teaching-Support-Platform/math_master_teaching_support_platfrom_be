package com.fptu.math_master.entity;

import com.fptu.math_master.enums.StudentRoadmapProgressStatus;
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
    name = "student_roadmap_progresses",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_student_roadmap_progresses",
          columnNames = {"student_id", "roadmap_id"})
    },
    indexes = {
      @Index(name = "idx_student_roadmap_progress_student", columnList = "student_id"),
      @Index(name = "idx_student_roadmap_progress_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_student_roadmap_progress_current_topic", columnList = "current_topic_id"),
      @Index(name = "idx_student_roadmap_progress_status", columnList = "status")
    })
public class StudentRoadmapProgress extends BaseEntity {

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  @Column(name = "current_topic_id")
  private UUID currentTopicId;

  @Column(name = "suggested_start_topic_id")
  private UUID suggestedStartTopicId;

  @Column(name = "placement_submission_id")
  private UUID placementSubmissionId;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private StudentRoadmapProgressStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_topic_id", insertable = false, updatable = false)
  private RoadmapTopic currentTopic;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suggested_start_topic_id", insertable = false, updatable = false)
  private RoadmapTopic suggestedStartTopic;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_submission_id", insertable = false, updatable = false)
  private Submission placementSubmission;

  @PrePersist
  public void prePersist() {
    if (status == null) status = StudentRoadmapProgressStatus.NOT_STARTED;
  }
}
