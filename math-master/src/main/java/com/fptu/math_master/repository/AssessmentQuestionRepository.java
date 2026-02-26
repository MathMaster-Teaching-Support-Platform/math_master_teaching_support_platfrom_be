package com.fptu.math_master.repository;

import com.fptu.math_master.entity.AssessmentQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

  @Query(
      "SELECT aq FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId ORDER BY aq.orderIndex")
  List<AssessmentQuestion> findByAssessmentIdOrderByOrderIndex(
      @Param("assessmentId") UUID assessmentId);

  @Query(
      "SELECT aq FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId AND aq.questionId = :questionId")
  Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(
      @Param("assessmentId") UUID assessmentId, @Param("questionId") UUID questionId);

  @Query(
      "SELECT MAX(aq.orderIndex) FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId")
  Integer findMaxOrderIndex(@Param("assessmentId") UUID assessmentId);

  void deleteByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

  /** Remove ALL question entries for an assessment — used before re-populating from matrix. */
  @Modifying
  @Query("DELETE FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId")
  void deleteAllByAssessmentId(@Param("assessmentId") UUID assessmentId);

  /**
   * Bulk-delete all assessment_questions for a given assessment whose questionId is in the provided
   * list. Used during replaceExisting to detach old cell questions.
   */
  @Modifying
  @Query(
      "DELETE FROM AssessmentQuestion aq "
          + "WHERE aq.assessmentId = :assessmentId "
          + "AND aq.questionId IN :questionIds")
  void deleteByAssessmentIdAndQuestionIdIn(
      @Param("assessmentId") UUID assessmentId, @Param("questionIds") List<UUID> questionIds);

  /** Check existing question texts within an assessment to guard against duplicates. */
  @Query(
      "SELECT q.questionText FROM AssessmentQuestion aq "
          + "JOIN Question q ON aq.questionId = q.id "
          + "WHERE aq.assessmentId = :assessmentId AND q.deletedAt IS NULL")
  List<String> findExistingQuestionTextsByAssessmentId(@Param("assessmentId") UUID assessmentId);
}
