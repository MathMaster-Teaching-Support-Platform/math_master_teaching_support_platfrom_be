package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query("SELECT s FROM Submission s WHERE s.assessmentId = :assessmentId AND s.studentId = :studentId")
    Optional<Submission> findByAssessmentIdAndStudentId(
        @Param("assessmentId") UUID assessmentId,
        @Param("studentId") UUID studentId
    );

    @Query("SELECT s FROM Submission s WHERE s.assessmentId = :assessmentId AND s.studentId = :studentId " +
           "AND s.status = :status")
    Optional<Submission> findByAssessmentIdAndStudentIdAndStatus(
        @Param("assessmentId") UUID assessmentId,
        @Param("studentId") UUID studentId,
        @Param("status") SubmissionStatus status
    );

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assessmentId = :assessmentId")
    Long countByAssessmentId(@Param("assessmentId") UUID assessmentId);
}

