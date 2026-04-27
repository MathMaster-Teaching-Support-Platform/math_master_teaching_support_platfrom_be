package com.fptu.math_master.repository;

import com.fptu.math_master.entity.WithdrawalRequest;
import com.fptu.math_master.enums.WithdrawalStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {

  /** User history — all statuses */
  Page<WithdrawalRequest> findByUserId(UUID userId, Pageable pageable);

  /** User history — filtered by status */
  Page<WithdrawalRequest> findByUserIdAndStatus(
      UUID userId, WithdrawalStatus status, Pageable pageable);

  /** Security guard: load only if the request belongs to the calling user */
  Optional<WithdrawalRequest> findByIdAndUserId(UUID id, UUID userId);

  /** Check if the user already has an unresolved request */
  boolean existsByUserIdAndStatusIn(UUID userId, List<WithdrawalStatus> statuses);

  /** Scheduler: find PENDING_VERIFY requests whose OTP has expired */
  @Query("""
      SELECT w FROM WithdrawalRequest w
      WHERE w.status = com.fptu.math_master.enums.WithdrawalStatus.PENDING_VERIFY
        AND w.otpExpiry < :now
      """)
  List<WithdrawalRequest> findExpiredPendingVerify(@Param("now") Instant now);

  /**
   * Admin list with optional status filter and free-text search across
   * userName, userEmail, and bankAccountNumber.
   */
  @Query("""
      SELECT w FROM WithdrawalRequest w
      JOIN FETCH w.user u
      WHERE (:status IS NULL OR w.status = :status)
        AND (:search IS NULL
             OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(w.bankAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
  Page<WithdrawalRequest> findAllForAdmin(
      @Param("status") WithdrawalStatus status,
      @Param("search") String search,
      Pageable pageable);
}
