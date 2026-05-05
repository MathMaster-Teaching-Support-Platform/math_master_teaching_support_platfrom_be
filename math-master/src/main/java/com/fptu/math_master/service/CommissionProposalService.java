package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CommissionProposalRequest;
import com.fptu.math_master.dto.request.ReviewCommissionProposalRequest;
import com.fptu.math_master.dto.response.CommissionProposalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface CommissionProposalService {

  // ── Teacher-facing ───────────────────────────────────────────────────────

  /**
   * Submit a new commission-split proposal for the authenticated teacher.
   * Throws if a PENDING proposal already exists.
   */
  CommissionProposalResponse submitProposal(CommissionProposalRequest request);

  /** Returns a paginated history of proposals for the authenticated teacher. */
  Page<CommissionProposalResponse> getMyProposals(Pageable pageable);

  /**
   * Returns the currently-active (most-recently-approved) proposal for the
   * authenticated teacher, or null when the platform default (90/10) applies.
   */
  CommissionProposalResponse getMyActiveRate();

  // ── Admin-facing ─────────────────────────────────────────────────────────

  /**
   * Returns a paginated list of proposals.
   * Pass {@code status = null} to retrieve all, or supply a specific status to filter.
   */
  Page<CommissionProposalResponse> getAllProposals(
      com.fptu.math_master.enums.CommissionProposalStatus status, Pageable pageable);

  /**
   * Approve or reject a proposal.
   * Sends an in-app notification to the teacher.
   */
  CommissionProposalResponse reviewProposal(UUID proposalId, ReviewCommissionProposalRequest request);

  // ── Internal helper used by OrderServiceImpl / EnrollmentServiceImpl ─────

  /**
   * Returns the active teacher share for the given teacher ID.
   * Falls back to 0.90 when no approved proposal exists.
   */
  BigDecimal getActiveTeacherShare(UUID teacherId);
}
