package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.enums.AssessmentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentRepository
    extends JpaRepository<Assessment, UUID>, JpaSpecificationExecutor<Assessment> {

  @Query("SELECT a FROM Assessment a WHERE a.id = :id AND a.deletedAt IS NULL")
  Optional<Assessment> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query("SELECT a FROM Assessment a WHERE a.teacherId = :teacherId AND a.deletedAt IS NULL")
  Page<Assessment> findByTeacherIdAndNotDeleted(
      @Param("teacherId") UUID teacherId, Pageable pageable);

  @Query("SELECT a FROM Assessment a WHERE a.deletedAt IS NULL")
  Page<Assessment> findByNotDeleted(Pageable pageable);

    @Query(
            "SELECT a FROM Assessment a WHERE a.deletedAt IS NULL "
                    + "AND LOWER(a.title) LIKE LOWER(CONCAT('%', :name, '%')) "
                    + "AND (:status IS NULL OR a.status = :status) "
                    + "ORDER BY a.createdAt DESC")
    List<Assessment> findByTitleContainingAndStatusAndNotDeleted(
            @Param("name") String name, @Param("status") AssessmentStatus status);

  @Query("SELECT a FROM Assessment a WHERE a.examMatrixId = :examMatrixId AND a.deletedAt IS NULL")
  List<Assessment> findByExamMatrixIdAndNotDeleted(@Param("examMatrixId") UUID examMatrixId);

  @Query("SELECT COUNT(s) FROM Submission s WHERE s.assessmentId = :assessmentId")
  Long countSubmissionsByAssessmentId(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT COUNT(aq) FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId")
  Long countQuestionsByAssessmentId(@Param("assessmentId") UUID assessmentId);

  @Query(
      "SELECT COALESCE(SUM(CASE WHEN aq.pointsOverride IS NOT NULL THEN aq.pointsOverride ELSE q.points END), 0) "
          + "FROM AssessmentQuestion aq "
          + "JOIN Question q ON aq.questionId = q.id "
          + "WHERE aq.assessmentId = :assessmentId")
  Double calculateTotalPoints(@Param("assessmentId") UUID assessmentId);

  /**
   * Bulk summary for a collection of assessment IDs.
   * Returns Object[] rows: [assessmentId, questionCount, totalPoints, submissionCount]
   * Used by mapToResponse to avoid N+1 queries when rendering a page of assessments.
   */
  @Query(
      "SELECT aq.assessmentId, "
          + "COUNT(DISTINCT aq.id), "
          + "COALESCE(SUM(CASE WHEN aq.pointsOverride IS NOT NULL THEN aq.pointsOverride ELSE q.points END), 0), "
          + "COUNT(DISTINCT s.id) "
          + "FROM AssessmentQuestion aq "
          + "JOIN Question q ON aq.questionId = q.id "
          + "LEFT JOIN Submission s ON s.assessmentId = aq.assessmentId "
          + "WHERE aq.assessmentId IN :ids "
          + "GROUP BY aq.assessmentId")
  List<Object[]> findBulkSummaryByIds(@Param("ids") Collection<UUID> ids);

  @Query(
      "SELECT a FROM Assessment a "
          + "WHERE a.deletedAt IS NULL "
          + "AND a.teacherId = :teacherId "
          + "AND (:status IS NULL OR a.status = :status) ")
  Page<Assessment> findWithFilters(
      @Param("teacherId") UUID teacherId,
      @Param("status") AssessmentStatus status,
      Pageable pageable);

  @Query(
      "SELECT a FROM Assessment a "
          + "WHERE a.status = 'PUBLISHED' "
          + "AND a.endDate IS NOT NULL "
          + "AND a.endDate < :now "
          + "AND a.deletedAt IS NULL")
  List<Assessment> findPublishedAssessmentsWithExpiredEndDate(@Param("now") Instant now);
}
