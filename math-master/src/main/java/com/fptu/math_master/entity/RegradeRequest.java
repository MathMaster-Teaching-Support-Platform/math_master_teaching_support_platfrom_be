package com.fptu.math_master.entity;

import com.fptu.math_master.enums.RegradeRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "regrade_requests",
    indexes = {
      @Index(name = "idx_regrade_submission", columnList = "submission_id"),
      @Index(name = "idx_regrade_student", columnList = "student_id"),
      @Index(name = "idx_regrade_status", columnList = "status"),
      @Index(name = "idx_regrade_question", columnList = "question_id")
    })
/**
 * The entity of 'RegradeRequest'.
 */
public class RegradeRequest extends BaseEntity {

  /**
   * submission_id
   */
  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  /**
   * question_id
   */
  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  /**
   * student_id
   */
  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  /**
   * reason
   */
  @Lob
  @Nationalized
  @Column(name = "reason", nullable = false)
  private String reason;

  /**
   * status
   */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private RegradeRequestStatus status;

  /**
   * teacher_response
   */
  @Lob
  @Nationalized
  @Column(name = "teacher_response")
  private String teacherResponse;

  /**
   * reviewed_by
   */
  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  /**
   * reviewed_at
   */
  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  /**
   * Relationships
   * - Many-to-One with Submission
   * - Many-to-One with Question
   * - Many-to-One with User (student)
   * - Many-to-One with User (reviewer)
   */
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
  }
}
