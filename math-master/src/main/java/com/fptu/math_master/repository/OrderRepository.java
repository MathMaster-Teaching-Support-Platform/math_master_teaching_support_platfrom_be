package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Order;
import com.fptu.math_master.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndStudentId(UUID id, UUID studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.studentId = :studentId")
    Optional<Order> findByIdAndStudentIdWithLock(@Param("id") UUID id, @Param("studentId") UUID studentId);

    Page<Order> findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    List<Order> findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
        UUID studentId, UUID courseId, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.expiresAt < :now AND o.deletedAt IS NULL")
    List<Order> findExpiredOrders(@Param("status") OrderStatus status, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus, o.cancelledAt = :now, o.cancellationReason = :reason " +
           "WHERE o.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<UUID> ids, 
                          @Param("newStatus") OrderStatus newStatus,
                          @Param("now") Instant now,
                          @Param("reason") String reason);

    long countByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.courseId = :courseId AND o.status = 'COMPLETED' AND o.deletedAt IS NULL")
    long countCompletedOrdersByCourse(@Param("courseId") UUID courseId);

    // Admin Financial Dashboard queries
    List<Order> findByStatusAndDeletedAtIsNull(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.confirmedAt BETWEEN :start AND :end AND o.deletedAt IS NULL")
    List<Order> findByStatusAndConfirmedAtBetweenAndDeletedAtIsNull(
        @Param("status") OrderStatus status,
        @Param("start") Instant start,
        @Param("end") Instant end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.confirmedAt BETWEEN :start AND :end AND o.deletedAt IS NULL")
    long countByStatusAndConfirmedAtBetweenAndDeletedAtIsNull(
        @Param("status") OrderStatus status,
        @Param("start") Instant start,
        @Param("end") Instant end);

    List<Order> findByCourseIdAndStatusAndDeletedAtIsNull(UUID courseId, OrderStatus status);

    long countByCourseIdInAndStatusAndDeletedAtIsNull(List<UUID> courseIds, OrderStatus status);
}