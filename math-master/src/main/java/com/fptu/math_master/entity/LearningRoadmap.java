package com.fptu.math_master.entity;

import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

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
@Data
@Entity
@Table(
    name = "learning_roadmaps",
    indexes = {
      @Index(name = "idx_roadmaps_student", columnList = "student_id"),
      @Index(name = "idx_roadmaps_student_subject", columnList = "student_id, subject"),
      @Index(name = "idx_roadmaps_status", columnList = "status"),
      @Index(name = "idx_roadmaps_grade_level", columnList = "grade_level"),
      @Index(name = "idx_roadmaps_created_at", columnList = "created_at")
    })
public class LearningRoadmap {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "teacher_id")
  private UUID teacherId; // If roadmap was assigned/customized by teacher

  @Size(max = 100)
  @Column(name = "subject", length = 100, nullable = false)
  private String subject; // e.g., "Algebra", "Geometry", "Trigonometry"

  @Size(max = 50)
  @Column(name = "grade_level", length = 50, nullable = false)
  private String gradeLevel; // e.g., "Grade 9", "Grade 10"

  @Column(name = "generation_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private RoadmapGenerationType generationType; // PERSONALIZED, DEFAULT, TEACHER_ASSIGNED

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private RoadmapStatus status; // GENERATED, IN_PROGRESS, COMPLETED, ARCHIVED

  @Column(name = "progress_percentage", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercentage; // 0-100

  @Column(name = "completed_topics_count", nullable = false)
  private Integer completedTopicsCount = 0;

  @Column(name = "total_topics_count", nullable = false)
  private Integer totalTopicsCount = 0;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description; // Optional description of the roadmap

  @Column(name = "estimated_completion_days")
  private Integer estimatedCompletionDays; // Estimated days to complete

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt; // Soft delete

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @OneToMany(
      mappedBy = "roadmap",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<RoadmapTopic> roadmapTopics;

  @PrePersist
  public void prePersist() {
    if (status == null) status = RoadmapStatus.GENERATED;
    if (progressPercentage == null) progressPercentage = java.math.BigDecimal.ZERO;
    if (completedTopicsCount == null) completedTopicsCount = 0;
    if (totalTopicsCount == null) totalTopicsCount = 0;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
