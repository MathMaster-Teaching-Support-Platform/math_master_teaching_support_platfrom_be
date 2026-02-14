package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "grade_audit_logs",
  indexes = {
    @Index(name = "idx_grade_audit_answer", columnList = "answer_id"),
    @Index(name = "idx_grade_audit_submission", columnList = "submission_id"),
    @Index(name = "idx_grade_audit_teacher", columnList = "teacher_id")
  }
)
public class GradeAuditLog {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "submission_id")
  private UUID submissionId;

  @Column(name = "answer_id")
  private UUID answerId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "old_points", precision = 5, scale = 2)
  private BigDecimal oldPoints;

  @Column(name = "new_points", precision = 5, scale = 2)
  private BigDecimal newPoints;

  @Lob
  @Nationalized
  @Column(name = "reason")
  private String reason;

  @Column(name = "created_at")
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id", insertable = false, updatable = false)
  private Answer answer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
  }
}

