package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CommissionProposalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a commission-split proposal.
 * Returned to both teachers (own proposals) and admins (all proposals).
 */
@Data
@Builder
public class CommissionProposalResponse {

  private UUID id;

  private UUID teacherId;
  private String teacherName;
  private String teacherEmail;

  /** The share the teacher requested to keep (e.g. 0.8000 = 80 %). */
  private BigDecimal teacherShare;

  /** Platform's share (1 - teacherShare). */
  private BigDecimal platformShare;

  private CommissionProposalStatus status;

  private String adminNote;
  private UUID reviewedBy;
  private Instant reviewedAt;

  private Instant createdAt;
  private Instant updatedAt;
}
