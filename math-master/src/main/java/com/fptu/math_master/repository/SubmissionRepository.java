package com.fptu.math_master.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.SubmissionStatus;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

  @Query(
      value =
          "SELECT * FROM submissions s "
              + "WHERE s.assessment_id = :assessmentId AND s.student_id = :studentId "
              + "ORDER BY s.created_at DESC LIMIT 1",
      nativeQuery = true)
  Optional<Submission> findByAssessmentIdAndStudentId(
      @Param("assessmentId") UUID assessmentId, @Param("studentId") UUID studentId);

  @Query(
      value =
          "SELECT * FROM submissions s "
              + "WHERE s.assessment_id = :assessmentId AND s.student_id = :studentId "
              + "AND s.status = CAST(:status AS VARCHAR) "
              + "ORDER BY s.created_at DESC LIMIT 1",
      nativeQuery = true)
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

  @Query(
      "SELECT COUNT(s) FROM Submission s "
          + "JOIN s.assessment a "
          + "WHERE a.teacherId = :teacherId AND s.status = :status")
  Long countByTeacherIdAndStatus(
      @Param("teacherId") UUID teacherId, @Param("status") SubmissionStatus status);

  @Query(
      "SELECT s FROM Submission s WHERE s.studentId = :studentId "
          + "AND s.status IN :statuses")
  List<Submission> findByStudentIdAndStatuses(
      @Param("studentId") UUID studentId,
      @Param("statuses") Collection<SubmissionStatus> statuses);

  long countByStudentIdAndStatusInAndDeletedAtIsNull(
      UUID studentId, Collection<SubmissionStatus> statuses);

  @Query(
      "SELECT AVG(COALESCE(s.finalScore, s.score, s.percentage)) FROM Submission s "
          + "WHERE s.studentId = :studentId "
          + "AND s.status = com.fptu.math_master.enums.SubmissionStatus.GRADED "
          + "AND s.deletedAt IS NULL")
  Double averageScoreOfGradedByStudentId(@Param("studentId") UUID studentId);

  List<Submission> findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
      UUID studentId, SubmissionStatus status);

  @Query(
      "SELECT s FROM Submission s "
          + "WHERE s.studentId = :studentId "
          + "AND s.assessmentId IN :assessmentIds "
          + "AND s.deletedAt IS NULL "
          + "ORDER BY s.createdAt DESC")
  List<Submission> findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(
      @Param("studentId") UUID studentId, @Param("assessmentIds") Collection<UUID> assessmentIds);

  @Query(
      "SELECT s.submittedAt, COALESCE(s.timeSpentSeconds, 0) FROM Submission s "
          + "WHERE s.studentId = :studentId "
          + "AND s.deletedAt IS NULL "
          + "AND s.submittedAt IS NOT NULL "
          + "AND s.submittedAt >= :from "
          + "AND s.submittedAt < :to")
  List<Object[]> findSubmissionActivityForWindow(
      @Param("studentId") UUID studentId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(
      "SELECT s.submittedAt FROM Submission s "
          + "WHERE s.studentId = :studentId "
          + "AND s.deletedAt IS NULL "
          + "AND s.submittedAt IS NOT NULL "
          + "AND s.submittedAt >= :from")
  List<Instant> findSubmittedAtAfter(
      @Param("studentId") UUID studentId,
      @Param("from") Instant from);
}
