package com.fptu.math_master.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

  Optional<Transaction> findByOrderCode(Long orderCode);

  Page<Transaction> findByWalletIdAndStatus(
      UUID walletId, TransactionStatus status, Pageable pageable);

  /** All transactions across all wallets — admin view */
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user")
  Page<Transaction> findAllWithUser(Pageable pageable);

  /**
   * Admin filtered view: filter by status list and optional search on user full name,
   * user email, transaction description, or order code.
   */
  @Query(
      "SELECT t FROM Transaction t LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user u "
          + "WHERE t.status IN :statuses "
          + "AND (:search = '' "
          + "  OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:search), '%') "
          + "  OR LOWER(u.email) LIKE CONCAT('%', LOWER(:search), '%') "
          + "  OR LOWER(t.description) LIKE CONCAT('%', LOWER(:search), '%') "
          + "  OR CAST(t.orderCode AS string) LIKE CONCAT('%', :search, '%'))")
  Page<Transaction> findAllWithUserFiltered(
      @Param("statuses") List<TransactionStatus> statuses,
      @Param("search") String search,
      Pageable pageable);

  /** Fetch a single transaction with user info eagerly loaded — for detail endpoint */
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user WHERE t.id = :id")
  Optional<Transaction> findByIdWithUser(@Param("id") UUID id);

  /** Count transactions grouped by status list */
  long countByStatusIn(List<TransactionStatus> statuses);

  /** Sum of ALL SUCCESS transaction amounts (no time window) */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS'")
  BigDecimal sumAllSuccessfulRevenue();

  /** Sum of SUCCESS deposits in a given time window */
  @Query(
      "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
          + "WHERE t.status = 'SUCCESS' AND t.createdAt >= :from AND t.createdAt < :to")
  java.math.BigDecimal sumSuccessfulRevenue(
      @Param("from") Instant from, @Param("to") Instant to);

  /** Monthly revenue grouped by month for a given year */
  @Query(
      value =
          "SELECT EXTRACT(MONTH FROM created_at) AS month, COALESCE(SUM(amount), 0) AS revenue "
              + "FROM transactions "
              + "WHERE status = 'SUCCESS' AND EXTRACT(YEAR FROM created_at) = :year "
              + "GROUP BY EXTRACT(MONTH FROM created_at) "
              + "ORDER BY month",
      nativeQuery = true)
  List<Object[]> sumRevenueByMonth(@Param("year") int year);

  long countByCreatedAtBetween(Instant from, Instant to);

  /** Total deposited (SUCCESS deposits) for a wallet */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.type = :type AND t.status = :status")
  BigDecimal sumByWalletIdAndTypeAndStatus(
      @Param("walletId") UUID walletId,
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status);

  /** Total transaction count for a wallet */
  long countByWalletId(UUID walletId);

  /** Find PENDING transactions whose expiresAt is in the past */
  @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.expiresAt IS NOT NULL AND t.expiresAt < :now")
  List<Transaction> findExpiredPendingTransactions(@Param("now") Instant now);

  /** Sum transactions by wallet, type, status, and date range */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
         "WHERE t.wallet.id = :walletId AND t.type = :type AND t.status = :status " +
         "AND t.createdAt >= :startDate AND t.createdAt <= :endDate")
  BigDecimal sumByWalletIdAndTypeAndStatusAndDateRange(
      @Param("walletId") UUID walletId,
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Find transactions by wallet and type with pagination */
  Page<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type, Pageable pageable);

  /** Sum instructor revenue for a specific course */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
         "WHERE t.wallet.id = :walletId AND t.order.courseId = :courseId " +
         "AND t.type = 'INSTRUCTOR_REVENUE' AND t.status = :status")
  BigDecimal sumInstructorRevenueForCourse(
      @Param("walletId") UUID walletId,
      @Param("courseId") UUID courseId,
      @Param("status") TransactionStatus status);

  /** Sum transactions by type, status, and date range */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
         "WHERE t.type = :type AND t.status = :status " +
         "AND t.createdAt >= :startDate AND t.createdAt <= :endDate")
  BigDecimal sumByTypeAndStatusAndDateRange(
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Count transactions by status */
  long countByStatus(TransactionStatus status);

  /** Count transactions by status and date range */
  long countByStatusAndCreatedAtBetween(TransactionStatus status, Instant start, Instant end);

  /** Count transactions by status list and date range */
  long countByStatusInAndCreatedAtBetween(List<TransactionStatus> statuses, Instant start, Instant end);

  /** Find transactions by date range */
  List<Transaction> findByCreatedAtBetween(Instant start, Instant end);
}
