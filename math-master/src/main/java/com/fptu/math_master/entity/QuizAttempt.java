package com.fptu.math_master.entity;

import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "quiz_attempts",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_quiz_attempts", columnNames = {"submission_id", "attempt_number"})
  },
  indexes = {
    @Index(name = "idx_quiz_attempts_submission", columnList = "submission_id"),
    @Index(name = "idx_quiz_attempts_assessment", columnList = "assessment_id"),
    @Index(name = "idx_quiz_attempts_student", columnList = "student_id"),
    @Index(name = "idx_quiz_attempts_number", columnList = "attempt_number"),
    @Index(name = "idx_quiz_attempts_status", columnList = "status")
  }
)
public class QuizAttempt {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "attempt_number", nullable = false)
  private Integer attemptNumber;

  @Column(name = "score", precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "max_score", precision = 5, scale = 2)
  private BigDecimal maxScore;

  @Column(name = "percentage", precision = 5, scale = 2)
  private BigDecimal percentage;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private SubmissionStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "time_spent_seconds")
  private Integer timeSpentSeconds;

  @Column(name = "ip_address", columnDefinition = "inet")
  private String ipAddress;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

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
    if (status == null) status = SubmissionStatus.IN_PROGRESS;
    if (startedAt == null) startedAt = Instant.now();
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

