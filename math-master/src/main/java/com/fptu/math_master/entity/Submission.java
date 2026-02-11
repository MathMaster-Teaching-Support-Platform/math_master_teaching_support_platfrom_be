package com.fptu.math_master.entity;

import com.fptu.math_master.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "submissions",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_submissions", columnNames = {"assessment_id", "student_id"})
  },
  indexes = {
    @Index(name = "idx_submissions_assessment", columnList = "assessment_id"),
    @Index(name = "idx_submissions_student", columnList = "student_id"),
    @Index(name = "idx_submissions_status", columnList = "status"),
    @Index(name = "idx_submissions_student_status", columnList = "student_id, status")
  }
)
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private SubmissionStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "score", precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "max_score", precision = 5, scale = 2)
  private BigDecimal maxScore;

  @Column(name = "percentage", precision = 5, scale = 2)
  private BigDecimal percentage;

  @Column(name = "time_spent_seconds")
  private Integer timeSpentSeconds;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

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

