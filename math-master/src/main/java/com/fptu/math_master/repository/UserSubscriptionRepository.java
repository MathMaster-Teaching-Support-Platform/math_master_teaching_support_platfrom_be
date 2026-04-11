package com.fptu.math_master.repository;

import com.fptu.math_master.entity.UserSubscription;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

  long countByPlanIdAndStatus(UUID planId, UserSubscriptionStatus status);

  long countByPlanIdAndStatusAndCreatedAtBetween(
      UUID planId, UserSubscriptionStatus status, Instant from, Instant to);

  /** Sum of amounts for a given plan in a given period */
  @Query(
      "SELECT COALESCE(SUM(s.amount), 0) FROM UserSubscription s "
          + "WHERE s.plan.id = :planId AND s.status = 'ACTIVE' "
          + "AND s.createdAt >= :from AND s.createdAt < :to")
  BigDecimal sumRevenueByPlan(
      @Param("planId") UUID planId, @Param("from") Instant from, @Param("to") Instant to);

  /** Total revenue across all plans for a period */
  @Query(
      "SELECT COALESCE(SUM(s.amount), 0) FROM UserSubscription s "
          + "WHERE s.status = 'ACTIVE' AND s.createdAt >= :from AND s.createdAt < :to")
  BigDecimal sumTotalRevenue(@Param("from") Instant from, @Param("to") Instant to);

  /** Count paid (non-zero) subscriptions in period */
  @Query(
      "SELECT COUNT(s) FROM UserSubscription s "
          + "WHERE s.status = 'ACTIVE' AND s.amount > 0 "
          + "AND s.createdAt >= :from AND s.createdAt < :to")
  long countPaidSubscriptions(@Param("from") Instant from, @Param("to") Instant to);

  /** All active paid subscriptions (no time filter) */
  @Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.status = 'ACTIVE' AND s.amount > 0")
  long countAllActivePaidSubscriptions();

  /** Paginated list for admin — optionally filter by status and/or planId */
  @Query(
      "SELECT s FROM UserSubscription s JOIN FETCH s.plan "
          + "WHERE s.deletedAt IS NULL "
          + "AND (:status IS NULL OR s.status = :status) "
          + "AND (:planId IS NULL OR s.plan.id = :planId)")
  Page<UserSubscription> findAllForAdmin(
      @Param("status") UserSubscriptionStatus status,
      @Param("planId") UUID planId,
      Pageable pageable);

  boolean existsByPlanIdAndStatus(UUID planId, UserSubscriptionStatus status);
}
