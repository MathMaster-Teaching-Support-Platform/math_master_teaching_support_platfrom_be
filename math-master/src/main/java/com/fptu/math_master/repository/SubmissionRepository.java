package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.SubmissionStatus;
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
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

  @Query(
      "SELECT s FROM Submission s WHERE s.assessmentId = :assessmentId AND s.studentId = :studentId")
  Optional<Submission> findByAssessmentIdAndStudentId(
      @Param("assessmentId") UUID assessmentId, @Param("studentId") UUID studentId);

  @Query(
      "SELECT s FROM Submission s WHERE s.assessmentId = :assessmentId AND s.studentId = :studentId "
          + "AND s.status = :status")
  Optional<Submission> findByAssessmentIdAndStudentIdAndStatus(
      @Param("assessmentId") UUID assessmentId,
      @Param("studentId") UUID studentId,
      @Param("status") SubmissionStatus status);

  @Query("SELECT COUNT(s) FROM Submission s WHERE s.assessmentId = :assessmentId")
  Long countByAssessmentId(@Param("assessmentId") UUID assessmentId);

  // Grading Queue - get submissions that need grading
  @Query("SELECT s FROM Submission s WHERE s.status = :status ORDER BY s.submittedAt ASC")
  Page<Submission> findByStatus(@Param("status") SubmissionStatus status, Pageable pageable);

  @Query(
      "SELECT s FROM Submission s JOIN s.assessment a WHERE a.teacherId = :teacherId "
          + "AND s.status = :status ORDER BY s.submittedAt ASC")
  Page<Submission> findByTeacherIdAndStatus(
      @Param("teacherId") UUID teacherId,
      @Param("status") SubmissionStatus status,
      Pageable pageable);

  // Get all submissions for an assessment
  @Query(
      "SELECT s FROM Submission s WHERE s.assessmentId = :assessmentId ORDER BY s.submittedAt DESC")
  List<Submission> findAllByAssessmentId(@Param("assessmentId") UUID assessmentId);

  // Count submissions by status for an assessment
  @Query(
      "SELECT COUNT(s) FROM Submission s WHERE s.assessmentId = :assessmentId AND s.status = :status")
  Long countByAssessmentIdAndStatus(
      @Param("assessmentId") UUID assessmentId, @Param("status") SubmissionStatus status);
}
