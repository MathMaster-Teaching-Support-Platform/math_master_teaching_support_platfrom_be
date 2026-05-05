package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CommissionProposal;
import com.fptu.math_master.enums.CommissionProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionProposalRepository extends JpaRepository<CommissionProposal, UUID> {

  /**
   * Returns the most-recently-approved proposal for a given teacher.
   * Used by OrderServiceImpl and EnrollmentServiceImpl to determine the active rate.
   */
  Optional<CommissionProposal> findTopByTeacherIdAndStatusOrderByReviewedAtDesc(
      UUID teacherId, CommissionProposalStatus status);

  /**
   * Returns the most-recently-created proposal with a given status for a teacher.
   * Used to check whether a PENDING proposal already exists before allowing submission.
   */
  Optional<CommissionProposal> findFirstByTeacherIdAndStatusOrderByCreatedAtDesc(
      UUID teacherId, CommissionProposalStatus status);

  /** Paginated list of proposals filtered by status (admin view). */
  Page<CommissionProposal> findByStatusOrderByCreatedAtDesc(
      CommissionProposalStatus status, Pageable pageable);

  /** Paginated list of all proposals (admin view, no status filter). */
  Page<CommissionProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /** Paginated list of proposals for a specific teacher (teacher history view). */
  Page<CommissionProposal> findByTeacherIdOrderByCreatedAtDesc(
      UUID teacherId, Pageable pageable);
}
