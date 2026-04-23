package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RefundRequest;
import com.fptu.math_master.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Optional<RefundRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<RefundRequest> findByEnrollmentIdAndDeletedAtIsNull(UUID enrollmentId);

    Page<RefundRequest> findByStudentIdAndDeletedAtIsNullOrderByRequestedAtDesc(
        UUID studentId, Pageable pageable);

    Page<RefundRequest> findByStatusAndDeletedAtIsNullOrderByRequestedAtDesc(
        RefundStatus status, Pageable pageable);

    Page<RefundRequest> findByStatusAndDeletedAtIsNullOrderByRequestedAtAsc(
        RefundStatus status, Pageable pageable);

    Optional<RefundRequest> findByIdAndDeletedAtIsNull(UUID id);

    Optional<RefundRequest> findByIdAndStudentIdAndDeletedAtIsNull(UUID id, UUID studentId);

    @Query("SELECT r FROM RefundRequest r WHERE r.status = :status AND r.deletedAt IS NULL ORDER BY r.requestedAt ASC")
    List<RefundRequest> findByStatusForProcessing(@Param("status") RefundStatus status);

    long countByStatusAndDeletedAtIsNull(RefundStatus status);

    boolean existsByOrderIdAndStatusInAndDeletedAtIsNull(UUID orderId, List<RefundStatus> statuses);
}