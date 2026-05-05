package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CommissionProposalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A teacher's request to negotiate their revenue split with the platform.
 * <p>
 * Invariants:
 * <ul>
 *   <li>teacherShare + platformShare == 1.0000</li>
 *   <li>teacherShare is in [0.50, 0.97]  (platform always keeps at least 3 %)</li>
 *   <li>Only one PENDING proposal per teacher at a time</li>
 *   <li>The most recent APPROVED proposal is the one used when creating orders</li>
 * </ul>
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "commission_proposals",
    indexes = {
        @Index(name = "idx_commission_proposals_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_commission_proposals_status",     columnList = "status")
    }
)
public class CommissionProposal extends BaseEntity {

  /** Teacher who submitted this proposal. */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * The share the teacher requests to keep (e.g. 0.8000 = 80 %).
   * Stored with 4 decimal places for precision.
   */
  @Column(name = "teacher_share", nullable = false, precision = 5, scale = 4)
  private BigDecimal teacherShare;

  /**
   * Platform's share — always computed as 1 - teacherShare.
   * Stored redundantly for easy querying.
   */
  @Column(name = "platform_share", nullable = false, precision = 5, scale = 4)
  private BigDecimal platformShare;

  /** Current lifecycle state. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private CommissionProposalStatus status = CommissionProposalStatus.PENDING;

  /** Admin note (rejection reason or approval comment). */
  @Column(name = "admin_note", columnDefinition = "TEXT")
  private String adminNote;

  /** Admin who reviewed this proposal. */
  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  /** Timestamp when the admin acted on this proposal. */
  @Column(name = "reviewed_at")
  private Instant reviewedAt;
}
