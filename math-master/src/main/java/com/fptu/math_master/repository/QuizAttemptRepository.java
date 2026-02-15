package com.fptu.math_master.repository;

import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.enums.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

  @Query(
      "SELECT qa FROM QuizAttempt qa WHERE qa.submissionId = :submissionId ORDER BY qa.attemptNumber DESC")
  List<QuizAttempt> findBySubmissionIdOrderByAttemptNumberDesc(
      @Param("submissionId") UUID submissionId);

  @Query(
      "SELECT qa FROM QuizAttempt qa WHERE qa.submissionId = :submissionId AND qa.attemptNumber = :attemptNumber")
  Optional<QuizAttempt> findBySubmissionIdAndAttemptNumber(
      @Param("submissionId") UUID submissionId, @Param("attemptNumber") Integer attemptNumber);

  @Query(
      "SELECT qa FROM QuizAttempt qa WHERE qa.assessmentId = :assessmentId AND qa.studentId = :studentId "
          + "AND qa.status = :status ORDER BY qa.attemptNumber DESC")
  List<QuizAttempt> findByAssessmentIdAndStudentIdAndStatus(
      @Param("assessmentId") UUID assessmentId,
      @Param("studentId") UUID studentId,
      @Param("status") SubmissionStatus status);

  @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.submissionId = :submissionId")
  Integer countBySubmissionId(@Param("submissionId") UUID submissionId);

  @Query(
      "SELECT qa FROM QuizAttempt qa WHERE qa.status = 'IN_PROGRESS' "
          + "AND qa.startedAt < :expirationTime")
  List<QuizAttempt> findExpiredAttempts(@Param("expirationTime") Instant expirationTime);
}
