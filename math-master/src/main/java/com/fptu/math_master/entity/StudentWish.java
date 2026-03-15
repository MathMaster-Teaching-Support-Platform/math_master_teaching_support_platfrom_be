package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

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
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "student_wishes",
    indexes = {
      @Index(name = "idx_student_wishes_student", columnList = "student_id"),
      @Index(name = "idx_student_wishes_subject", columnList = "subject"),
      @Index(name = "idx_student_wishes_created", columnList = "created_at")
    })
public class StudentWish extends BaseEntity {

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  /**
   * subject
   */
  @Size(max = 100)
  @Column(name = "subject", length = 100, nullable = false)
  private String subject;

  /**
   * grade_level
   */
  @Column(name = "grade_level", length = 50)
  private String gradeLevel;

  /**
   * learning_goals
   */
  @Lob
  @Nationalized
  @Column(name = "learning_goals")
  private String learningGoals;

  /**
   * preferred_topics
   */
  @Lob
  @Nationalized
  @Column(name = "preferred_topics")
  private String preferredTopics;

  /**
   * weak_areas_to_improve
   */
  @Lob
  @Nationalized
  @Column(name = "weak_areas_to_improve")
  private String weakAreasToImprove;

  /**
   * daily_study_minutes
   */
  @Column(name = "daily_study_minutes", nullable = false)
  private Integer dailyStudyMinutes = 60;

  /**
   * target_accuracy_percentage
   */
  @Column(name = "target_accuracy_percentage")
  private Integer targetAccuracyPercentage;

  /**
   * learning_style_preference
   */
  @Lob
  @Nationalized
  @Column(name = "learning_style_preference")
  private String learningStylePreference;

  /**
   * prefer_difficult_challenges
   */
  @Column(name = "prefer_difficult_challenges")
  private Boolean preferDifficultChallenges = false;

  /**
   * is_active
   */
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  /**
   * Relationships
   * - Many-to-One with User (student)
   */
  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @PrePersist
  public void prePersist() {
    if (isActive == null) isActive = true;
    if (dailyStudyMinutes == null) dailyStudyMinutes = 60;
    if (preferDifficultChallenges == null) preferDifficultChallenges = false;
  }
}
