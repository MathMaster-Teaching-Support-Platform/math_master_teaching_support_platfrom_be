package com.fptu.math_master.entity;

import com.fptu.math_master.enums.SubmissionStatus;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "quiz_attempts",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_quiz_attempts",
          columnNames = {"submission_id", "attempt_number"})
    },
    indexes = {
      @Index(name = "idx_quiz_attempts_submission", columnList = "submission_id"),
      @Index(name = "idx_quiz_attempts_assessment", columnList = "assessment_id"),
      @Index(name = "idx_quiz_attempts_student", columnList = "student_id"),
      @Index(name = "idx_quiz_attempts_number", columnList = "attempt_number"),
      @Index(name = "idx_quiz_attempts_status", columnList = "status")
    })
/**
 * The entity of 'QuizAttempt'.
 */
public class QuizAttempt extends BaseEntity {

  /**
   * submission_id
   */
  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

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
   * attempt_number
   */
  @Column(name = "attempt_number", nullable = false)
  private Integer attemptNumber;

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
   * time_spent_seconds
   */
  @Column(name = "time_spent_seconds")
  private Integer timeSpentSeconds;

  /**
   * ip_address
   */
  @Column(name = "ip_address", columnDefinition = "inet")
  private String ipAddress;

  /**
   * Relationships
   * - Many-to-One with Submission
   * - Many-to-One with Assessment
   * - Many-to-One with User (student)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (status == null) status = SubmissionStatus.IN_PROGRESS;
    if (startedAt == null) startedAt = Instant.now();
  }
}
