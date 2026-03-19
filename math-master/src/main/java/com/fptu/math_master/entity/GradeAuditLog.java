package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    name = "grade_audit_logs",
    indexes = {
      @Index(name = "idx_grade_audit_answer", columnList = "answer_id"),
      @Index(name = "idx_grade_audit_submission", columnList = "submission_id"),
      @Index(name = "idx_grade_audit_teacher", columnList = "teacher_id")
    })
/**
 * The entity of 'GradeAuditLog'.
 */
public class GradeAuditLog extends BaseEntity {

  /**
   * submission_id
   */
  @Column(name = "submission_id")
  private UUID submissionId;

  /**
   * answer_id
   */
  @Column(name = "answer_id")
  private UUID answerId;

  /**
   * teacher_id
   */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * old_points
   */
  @Column(name = "old_points", precision = 5, scale = 2)
  private BigDecimal oldPoints;

  /**
   * new_points
   */
  @Column(name = "new_points", precision = 5, scale = 2)
  private BigDecimal newPoints;

  /**
   * reason
   */
  @Nationalized
  @Column(name = "reason")
  private String reason;

  /**
   * Relationships
   * - Many-to-One with Submission
   * - Many-to-One with Answer
   * - Many-to-One with User (teacher)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id", insertable = false, updatable = false)
  private Answer answer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;
}
