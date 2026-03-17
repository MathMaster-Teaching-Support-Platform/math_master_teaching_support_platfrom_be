package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;

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

/**
 * Represents a personalized learning roadmap for a student
 *
 * <p>A roadmap consists of:
 * - Student reference
 * - Subject and grade level
 * - Generation type (personalized/default/teacher-assigned)
 * - Overall progress tracking
 * - Links to roadmap topics
 *
 * <p>Flow: Each student can have multiple roadmaps (one per subject or general).
 * Roadmaps are generated based on performance data or teacher assignments.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "learning_roadmaps",
    indexes = {
      @Index(name = "idx_roadmaps_student", columnList = "student_id"),
      @Index(name = "idx_roadmaps_student_subject", columnList = "student_id, subject"),
      @Index(name = "idx_roadmaps_subject_id", columnList = "subject_id"),
      @Index(name = "idx_roadmaps_status", columnList = "status"),
      @Index(name = "idx_roadmaps_grade_level", columnList = "grade_level"),
      @Index(name = "idx_roadmaps_created_at", columnList = "created_at")
    })
public class LearningRoadmap extends BaseEntity {

  @Column(name = "student_id", nullable = true)
  private UUID studentId;

  @Column(name = "teacher_id")
  private UUID teacherId;

  @Column(name = "subject_id")
  private UUID subjectId;

  /**
   * name - Display name for the roadmap (used for admin templates or custom naming).
   * Example: "Toán học lớp 6 cho người mới bắt đầu"
   */
  @Size(max = 255)
  @Column(name = "name", length = 255)
  private String name;

  /**
   * subject
   */
  @Size(max = 100)
  @Column(name = "subject", length = 100, nullable = false)
  private String subject;

  /**
   * grade_level
   */
  @Size(max = 50)
  @Column(name = "grade_level", length = 50, nullable = false)
  private String gradeLevel;

  /**
   * generation_type
   */
  @Column(name = "generation_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private RoadmapGenerationType generationType;

  /**
   * status
   */
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private RoadmapStatus status;

  /**
   * progress_percentage
   */
  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage;

  /**
   * completed_topics_count
   */
  @Builder.Default
  @Column(name = "completed_topics_count", nullable = false)
  private Integer completedTopicsCount = 0;

  /**
   * total_topics_count
   */
  @Builder.Default
  @Column(name = "total_topics_count", nullable = false)
  private Integer totalTopicsCount = 0;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * estimated_completion_days
   */
  @Column(name = "estimated_completion_days")
  private Integer estimatedCompletionDays;

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
   * - Many-to-One with User (student)
   * - Many-to-One with User (teacher)
   * - One-to-Many with RoadmapTopic
   */
  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subjectRef;

  @OneToMany(
      mappedBy = "roadmap",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<RoadmapTopic> roadmapTopics;

  @PrePersist
  @Override
  public void prePersist() {
    super.prePersist();
    if (status == null) status = RoadmapStatus.GENERATED;
    if (progressPercentage == null) progressPercentage = java.math.BigDecimal.ZERO;
    if (completedTopicsCount == null) completedTopicsCount = 0;
    if (totalTopicsCount == null) totalTopicsCount = 0;
  }
}
