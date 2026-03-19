package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.fptu.math_master.enums.SubmissionStatus;

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
import jakarta.persistence.UniqueConstraint;
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
    name = "submissions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_submissions",
          columnNames = {"assessment_id", "student_id"})
    },
    indexes = {
      @Index(name = "idx_submissions_assessment", columnList = "assessment_id"),
      @Index(name = "idx_submissions_student", columnList = "student_id"),
      @Index(name = "idx_submissions_status", columnList = "status"),
      @Index(name = "idx_submissions_student_status", columnList = "student_id, status")
    })
/**
 * The entity of 'Submission'.
 */
public class Submission extends BaseEntity {

  /**
   * assessment_id
   */
  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  /**
   * student_id
   */
  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  /**
   * status
   */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private SubmissionStatus status;

  /**
   * started_at
   */
  @Column(name = "started_at")
  private Instant startedAt;

  /**
   * submitted_at
   */
  @Column(name = "submitted_at")
  private Instant submittedAt;

  /**
   * score
   */
  @Column(name = "score", precision = 5, scale = 2)
  private BigDecimal score;

  /**
   * max_score
   */
  @Column(name = "max_score", precision = 5, scale = 2)
  private BigDecimal maxScore;

  /**
   * percentage
   */
  @Column(name = "percentage", precision = 5, scale = 2)
  private BigDecimal percentage;

  /**
   * time_spent_seconds
   */
  @Column(name = "time_spent_seconds")
  private Integer timeSpentSeconds;

  /**
   * manual_adjustment
   */
  @Column(name = "manual_adjustment", precision = 5, scale = 2)
  private BigDecimal manualAdjustment;

  /**
   * manual_adjustment_reason
   */
  @Column(name = "manual_adjustment_reason")
  private String manualAdjustmentReason;

  /**
   * final_score
   */
  @Column(name = "final_score", precision = 5, scale = 2)
  private BigDecimal finalScore;

  /**
   * grades_released
   */
  @Column(name = "grades_released")
  private Boolean gradesReleased;

  /**
   * graded_by
   */
  @Column(name = "graded_by")
  private UUID gradedBy;

  /**
   * graded_at
   */
  @Column(name = "graded_at")
  private Instant gradedAt;

  /**
   * Relationships
   * - Many-to-One with Assessment
   * - Many-to-One with User (student)
   * - One-to-Many with QuizAttempt
   * - One-to-Many with Answer
   * - One-to-Many with AiReview
   * - One-to-Many with GradeAuditLog
   * - One-to-Many with RegradeRequest
   * - Many-to-One with User (gradedBy)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuizAttempt> quizAttempts;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Answer> answers;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AiReview> aiReviews;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<GradeAuditLog> gradeAuditLogs;

  @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<RegradeRequest> regradeRequests;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "graded_by", insertable = false, updatable = false)
  private User grader;

  @PrePersist
  public void prePersist() {
    if (status == null) status = SubmissionStatus.IN_PROGRESS;
    if (startedAt == null) startedAt = Instant.now();
    if (gradesReleased == null) gradesReleased = false;
    if (manualAdjustment == null) manualAdjustment = BigDecimal.ZERO;
  }
}
