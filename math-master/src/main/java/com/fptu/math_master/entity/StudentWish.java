package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a student's learning goals, wishes, and preferences
 *
 * <p>Stores:
 * - Learning goals (e.g., improve weaker areas, master specific topics)
 * - Preferred topics and areas of interest
 * - Daily study time availability
 * - Target grade level or performance
 * - Study preferences and learning style
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "student_wishes",
    indexes = {
      @Index(name = "idx_student_wishes_student", columnList = "student_id"),
      @Index(name = "idx_student_wishes_subject", columnList = "subject"),
      @Index(name = "idx_student_wishes_created", columnList = "created_at")
    })
public class StudentWish {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId; // Reference to User (student)

  @Size(max = 100)
  @Column(name = "subject", length = 100, nullable = false)
  private String subject; // e.g., "Algebra", "Geometry"

  @Column(name = "grade_level", length = 50)
  private String gradeLevel; // e.g., "Grade 9", "Grade 10"

  @Lob
  @Nationalized
  @Column(name = "learning_goals")
  private String learningGoals; // Description of what student wants to achieve

  @Lob
  @Nationalized
  @Column(name = "preferred_topics")
  private String preferredTopics; // Comma-separated or JSON list of preferred topics

  @Lob
  @Nationalized
  @Column(name = "weak_areas_to_improve")
  private String weakAreasToImprove; // Topics student wants to focus on

  @Column(name = "daily_study_minutes", nullable = false)
  private Integer dailyStudyMinutes = 60; // Minutes per day student can study

  @Column(name = "target_accuracy_percentage")
  private Integer targetAccuracyPercentage; // e.g., 90% accuracy goal

  @Lob
  @Nationalized
  @Column(name = "learning_style_preference")
  private String learningStylePreference; // e.g., "visual", "practice", "theory-first"

  @Column(name = "prefer_difficult_challenges")
  private Boolean preferDifficultChallenges = false; // Whether student likes challenging problems

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @PrePersist
  public void prePersist() {
    if (isActive == null) isActive = true;
    if (dailyStudyMinutes == null) dailyStudyMinutes = 60;
    if (preferDifficultChallenges == null) preferDifficultChallenges = false;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
