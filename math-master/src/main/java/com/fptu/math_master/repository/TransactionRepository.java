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

  @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user WHERE t.orderCode = :orderCode")
  Optional<Transaction> findByOrderCodeWithUser(@Param("orderCode") Long orderCode);

  Page<Transaction> findByWalletIdAndStatus(
      UUID walletId, TransactionStatus status, Pageable pageable);

  /**
   * Sum signed balance delta of successful wallet transactions after a timestamp.
   * Positive: DEPOSIT, INSTRUCTOR_REVENUE. Negative: WITHDRAWAL, PAYMENT, COURSE_PURCHASE.
   */
  @Query(
      "SELECT COALESCE(SUM(CASE "
          + "WHEN t.type IN ('DEPOSIT', 'INSTRUCTOR_REVENUE') THEN t.amount "
          + "WHEN t.type IN ('WITHDRAWAL', 'PAYMENT', 'COURSE_PURCHASE') THEN -t.amount "
          + "ELSE 0 END), 0) "
          + "FROM Transaction t "
          + "WHERE t.wallet.id = :walletId "
          + "AND t.status = 'SUCCESS' "
          + "AND COALESCE(t.createdAt, t.transactionDate) > :effectiveTime")
  BigDecimal sumSuccessfulBalanceDeltaAfter(
      @Param("walletId") UUID walletId, @Param("effectiveTime") Instant effectiveTime);

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

  /** Sum of ALL SUCCESS platform revenue (PLATFORM_COMMISSION + PAYMENT) amounts (no time window) */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.type IN ('PLATFORM_COMMISSION', 'PAYMENT')")
  BigDecimal sumAllSuccessfulRevenue();

  /** Sum of ALL SUCCESS platform revenue (PLATFORM_COMMISSION + PAYMENT) amounts in a given time window */
  @Query(
      "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
          + "WHERE t.status = 'SUCCESS' AND t.type IN ('PLATFORM_COMMISSION', 'PAYMENT') AND t.createdAt >= :from AND t.createdAt < :to")
  BigDecimal sumSuccessfulRevenue(
      @Param("from") Instant from, @Param("to") Instant to);

  /** Sum of PLATFORM revenue (COMMISSION + SUBSCRIPTION PAYMENTS) in a given time window */
  @Query(
      "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
          + "WHERE t.status = 'SUCCESS' "
          + "AND t.type IN ('PLATFORM_COMMISSION', 'PAYMENT') "
          + "AND t.createdAt >= :from AND t.createdAt < :to")
  BigDecimal sumSuccessfulPlatformRevenue(
      @Param("from") Instant from, @Param("to") Instant to);

  /** Sum of ALL successful deposits (cash inflow) */
  @Query(
      "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
          + "WHERE t.status = 'SUCCESS' AND t.type = 'DEPOSIT' "
          + "AND t.createdAt >= :from AND t.createdAt < :to")
  BigDecimal sumSuccessfulDeposits(
      @Param("from") Instant from, @Param("to") Instant to);

  /** Monthly platform revenue (PLATFORM_COMMISSION + PAYMENT) grouped by month for a given year */
  @Query(
      value =
          "SELECT EXTRACT(MONTH FROM created_at) AS month, COALESCE(SUM(amount), 0) AS revenue "
              + "FROM transactions "
              + "WHERE status = 'SUCCESS' AND type IN ('PLATFORM_COMMISSION', 'PAYMENT') AND EXTRACT(YEAR FROM created_at) = :year "
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

  /** Total amount for a wallet matching multiple types */
  @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.type IN :types AND t.status = :status")
  BigDecimal sumByWalletIdAndTypeInAndStatus(
      @Param("walletId") UUID walletId,
      @Param("types") List<TransactionType> types,
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

  /** Count transactions by status list, type list and date range */
  long countByStatusInAndTypeInAndCreatedAtBetween(
          List<TransactionStatus> statuses, List<TransactionType> types, Instant start, Instant end);

  /** Count unique users who had a successful transaction of a certain type */
  @Query("SELECT COUNT(DISTINCT t.wallet.user.id) FROM Transaction t " +
         "WHERE t.status = 'SUCCESS' AND t.type = :type " +
         "AND t.createdAt >= :from AND t.createdAt < :to")
  long countUniqueUsersByTypeAndDateRange(
      @Param("type") TransactionType type,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Find transactions by date range */
  List<Transaction> findByCreatedAtBetween(Instant start, Instant end);

  // ─── Cash Flow aggregation queries ───────────────────────────────────────────
  // INFLOW types:  DEPOSIT, PAYMENT, COURSE_PURCHASE
  // OUTFLOW types: WITHDRAWAL, INSTRUCTOR_REVENUE

  /**
   * Sum of INFLOW or OUTFLOW amounts for a date range.
   * type_list is a PostgreSQL array literal, e.g. 'DEPOSIT','PAYMENT','COURSE_PURCHASE'
   */
  @Query(value =
      "SELECT COALESCE(SUM(t.amount), 0) FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.type IN (:types) " +
      "  AND t.created_at >= :from AND t.created_at < :to",
      nativeQuery = true)
  BigDecimal sumCashFlowByTypesAndDateRange(
      @Param("types") List<String> types,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Daily inflow vs outflow — used for 'day' chart grouping.
   * Columns: [0]=label (YYYY-MM-DD), [1]=inflow, [2]=outflow
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('day', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('DEPOSIT','PAYMENT')), 0) AS inflow, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('WITHDRAWAL')), 0) AS outflow " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('day', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findCashFlowDailyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Hourly inflow vs outflow — used for 'hour' chart grouping.
   * Columns: [0]=label (YYYY-MM-DD HH:00), [1]=inflow, [2]=outflow
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('hour', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD HH24:00') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('DEPOSIT','PAYMENT')), 0) AS inflow, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('WITHDRAWAL')), 0) AS outflow " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('hour', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findCashFlowHourlyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Weekly inflow vs outflow.
   * Columns: [0]=label (YYYY-MM-DD of the week's Monday), [1]=inflow, [2]=outflow
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('week', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('DEPOSIT','PAYMENT')), 0) AS inflow, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('WITHDRAWAL')), 0) AS outflow " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('week', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findCashFlowWeeklyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Monthly inflow vs outflow.
   * Columns: [0]=label (YYYY-MM), [1]=inflow, [2]=outflow
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('month', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('DEPOSIT','PAYMENT')), 0) AS inflow, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type IN ('WITHDRAWAL')), 0) AS outflow " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('month', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findCashFlowMonthlyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Revenue breakdown grouped by hour.
   * Columns: [0]=label (YYYY-MM-DD HH:00), [1]=deposits, [2]=subscriptions, [3]=courseSales
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('hour', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD HH24:00') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'DEPOSIT'), 0) AS deposits, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PAYMENT'), 0) AS subscriptions, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PLATFORM_COMMISSION'), 0) AS course_sales " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('hour', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findRevenueBreakdownHourlyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Revenue breakdown grouped by day.
   * Columns: [0]=label (YYYY-MM-DD), [1]=deposits, [2]=subscriptions, [3]=courseSales
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('day', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'DEPOSIT'), 0) AS deposits, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PAYMENT'), 0) AS subscriptions, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PLATFORM_COMMISSION'), 0) AS course_sales " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('day', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findRevenueBreakdownDailyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Revenue breakdown grouped by month.
   * Columns: [0]=label (YYYY-MM), [1]=deposits, [2]=subscriptions, [3]=courseSales
   */
  @Query(value =
      "SELECT TO_CHAR(DATE_TRUNC('month', t.created_at AT TIME ZONE 'UTC'), 'YYYY-MM') AS label, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'DEPOSIT'), 0) AS deposits, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PAYMENT'), 0) AS subscriptions, " +
      "       COALESCE(SUM(t.amount) FILTER (WHERE t.type = 'PLATFORM_COMMISSION'), 0) AS course_sales " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "GROUP BY DATE_TRUNC('month', t.created_at AT TIME ZONE 'UTC') " +
      "ORDER BY label",
      nativeQuery = true)
  List<Object[]> findRevenueBreakdownMonthlyAggregates(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Category breakdown: sums per TransactionType within a date range.
   * Columns: [0]=type (TransactionType name), [1]=total
   */
  @Query(value =
      "SELECT t.type AS category, COALESCE(SUM(t.amount), 0) AS total " +
      "FROM transactions t " +
      "WHERE t.status = 'SUCCESS' " +
      "  AND t.created_at >= :from AND t.created_at < :to " +
      "  AND t.type IN ('DEPOSIT','PAYMENT','WITHDRAWAL') " +
      "GROUP BY t.type " +
      "ORDER BY total DESC",
      nativeQuery = true)
  List<Object[]> findCashFlowCategoryBreakdown(
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Paginated transactions for cash-flow view, filtered by type list and date range.
   * Eagerly fetches wallet + user for display purposes.
   */
  @Query(
      value = "SELECT t FROM Transaction t LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user u " +
              "WHERE t.status = 'SUCCESS' " +
              "AND t.type IN :types " +
              "AND t.createdAt >= :from AND t.createdAt < :to " +
              "AND (:search = '' " +
              "  OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:search), '%') " +
              "  OR LOWER(t.description) LIKE CONCAT('%', LOWER(:search), '%') " +
              "  OR CAST(t.orderCode AS string) LIKE CONCAT('%', :search, '%'))",
      countQuery = "SELECT COUNT(t) FROM Transaction t LEFT JOIN t.wallet w LEFT JOIN w.user u " +
                   "WHERE t.status = 'SUCCESS' " +
                   "AND t.type IN :types " +
                   "AND t.createdAt >= :from AND t.createdAt < :to " +
                   "AND (:search = '' " +
                   "  OR LOWER(u.fullName) LIKE CONCAT('%', LOWER(:search), '%') " +
                   "  OR LOWER(t.description) LIKE CONCAT('%', LOWER(:search), '%') " +
                   "  OR CAST(t.orderCode AS string) LIKE CONCAT('%', :search, '%'))")
  Page<Transaction> findCashFlowTransactions(
      @Param("types") List<TransactionType> types,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("search") String search,
      Pageable pageable);
}

