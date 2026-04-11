package com.fptu.math_master.repository;

import java.math.BigDecimal;
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

import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionStatus;

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
}
