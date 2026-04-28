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
   * Uses native SQL with ::text cast to avoid lower(bytea) errors when user
   * columns (username, email) are stored as bytea in PostgreSQL.
   */
  @Query(
      value = """
          SELECT wr.*
          FROM withdrawal_requests wr
          JOIN users u ON wr.user_id = u.id
          WHERE (:status IS NULL OR wr.status = :status)
            AND (:search IS NULL
                 OR u.username::text          ILIKE CONCAT('%', :search, '%')
                 OR u.email::text             ILIKE CONCAT('%', :search, '%')
                 OR wr.bank_account_number    ILIKE CONCAT('%', :search, '%'))
          ORDER BY wr.created_at DESC
          """,
      countQuery = """
          SELECT COUNT(*)
          FROM withdrawal_requests wr
          JOIN users u ON wr.user_id = u.id
          WHERE (:status IS NULL OR wr.status = :status)
            AND (:search IS NULL
                 OR u.username::text          ILIKE CONCAT('%', :search, '%')
                 OR u.email::text             ILIKE CONCAT('%', :search, '%')
                 OR wr.bank_account_number    ILIKE CONCAT('%', :search, '%'))
          """,
      nativeQuery = true)
  Page<WithdrawalRequest> findAllForAdmin(
      @Param("status") String status,
      @Param("search") String search,
      Pageable pageable);
}
