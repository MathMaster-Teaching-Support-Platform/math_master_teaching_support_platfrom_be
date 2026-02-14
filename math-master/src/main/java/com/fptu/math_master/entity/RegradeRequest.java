package com.fptu.math_master.entity;

import com.fptu.math_master.enums.RegradeRequestStatus;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "regrade_requests",
  indexes = {
    @Index(name = "idx_regrade_submission", columnList = "submission_id"),
    @Index(name = "idx_regrade_student", columnList = "student_id"),
    @Index(name = "idx_regrade_status", columnList = "status"),
    @Index(name = "idx_regrade_question", columnList = "question_id")
  }
)
public class RegradeRequest {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Lob
  @Nationalized
  @Column(name = "reason", nullable = false)
  private String reason;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private RegradeRequestStatus status;

  @Lob
  @Nationalized
  @Column(name = "teacher_response")
  private String teacherResponse;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by", insertable = false, updatable = false)
  private User reviewer;

  @PrePersist
  public void prePersist() {
    if (status == null) status = RegradeRequestStatus.PENDING;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

