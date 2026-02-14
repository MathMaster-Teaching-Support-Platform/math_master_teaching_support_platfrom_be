package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RegradeRequest;
import com.fptu.math_master.enums.RegradeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegradeRequestRepository extends JpaRepository<RegradeRequest, UUID> {

    @Query("SELECT r FROM RegradeRequest r WHERE r.submissionId = :submissionId ORDER BY r.createdAt DESC")
    List<RegradeRequest> findBySubmissionId(@Param("submissionId") UUID submissionId);

    @Query("SELECT r FROM RegradeRequest r WHERE r.studentId = :studentId ORDER BY r.createdAt DESC")
    Page<RegradeRequest> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    @Query("SELECT r FROM RegradeRequest r WHERE r.status = :status ORDER BY r.createdAt ASC")
    Page<RegradeRequest> findByStatus(@Param("status") RegradeRequestStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RegradeRequest r WHERE r.status = :status")
    Long countByStatus(@Param("status") RegradeRequestStatus status);
}

