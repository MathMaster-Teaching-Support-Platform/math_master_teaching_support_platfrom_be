package com.fptu.math_master.repository;

import com.fptu.math_master.dto.response.QuestionBankSummaryProjection;
import com.fptu.math_master.entity.QuestionBank;
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
public interface QuestionBankRepository
    extends JpaRepository<QuestionBank, UUID>, JpaSpecificationExecutor<QuestionBank> {

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.id = :id AND qb.deletedAt IS NULL")
  Optional<QuestionBank> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.teacherId = :teacherId AND qb.deletedAt IS NULL")
  Page<QuestionBank> findByTeacherIdAndNotDeleted(
      @Param("teacherId") UUID teacherId, Pageable pageable);

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.isPublic = true AND qb.deletedAt IS NULL")
  Page<QuestionBank> findPublicQuestionBanks(Pageable pageable);

  @Query(
      "SELECT qb FROM QuestionBank qb "
          + "WHERE qb.deletedAt IS NULL "
          + "AND (qb.teacherId = :teacherId OR qb.isPublic = true) "
          + "AND (:subject IS NULL OR qb.subject = :subject) "
          + "AND (:gradeLevel IS NULL OR qb.gradeLevel = :gradeLevel) "
          + "AND (:isPublic IS NULL OR qb.isPublic = :isPublic) "
          + "AND (:searchTerm IS NULL OR LOWER(qb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  Page<QuestionBank> findWithFilters(
      @Param("teacherId") UUID teacherId,
      @Param("subject") String subject,
      @Param("gradeLevel") String gradeLevel,
      @Param("isPublic") Boolean isPublic,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  @Query(
      "SELECT COUNT(q) FROM Question q WHERE q.questionBankId = :questionBankId AND q.deletedAt IS NULL")
  Long countQuestionsByQuestionBankId(@Param("questionBankId") UUID questionBankId);

  @Query(
      "SELECT COUNT(aq) > 0 FROM AssessmentQuestion aq "
          + "JOIN Question q ON aq.questionId = q.id "
          + "WHERE q.questionBankId = :questionBankId AND q.deletedAt IS NULL")
  boolean hasQuestionsInUse(@Param("questionBankId") UUID questionBankId);

  // -----------------------------------------------------------------------
  // Bulk-projection queries – fix N+1 on list/search endpoints
  // Each query returns teacher name + question count in a single round-trip.
  // -----------------------------------------------------------------------

  @Query(
      "SELECT qb.id              AS id, "
          + "qb.teacherId        AS teacherId, "
          + "u.fullName          AS teacherName, "
          + "qb.name             AS name, "
          + "qb.description      AS description, "
          + "qb.subject          AS subject, "
          + "qb.gradeLevel       AS gradeLevel, "
          + "qb.isPublic         AS isPublic, "
          + "(SELECT COUNT(q) FROM Question q "
          + "  WHERE q.questionBankId = qb.id AND q.deletedAt IS NULL) AS questionCount, "
          + "qb.createdAt        AS createdAt, "
          + "qb.updatedAt        AS updatedAt "
          + "FROM QuestionBank qb JOIN User u ON u.id = qb.teacherId "
          + "WHERE qb.teacherId = :teacherId AND qb.deletedAt IS NULL")
  Page<QuestionBankSummaryProjection> findSummaryByTeacherIdAndNotDeleted(
      @Param("teacherId") UUID teacherId, Pageable pageable);

  @Query(
      "SELECT qb.id              AS id, "
          + "qb.teacherId        AS teacherId, "
          + "u.fullName          AS teacherName, "
          + "qb.name             AS name, "
          + "qb.description      AS description, "
          + "qb.subject          AS subject, "
          + "qb.gradeLevel       AS gradeLevel, "
          + "qb.isPublic         AS isPublic, "
          + "(SELECT COUNT(q) FROM Question q "
          + "  WHERE q.questionBankId = qb.id AND q.deletedAt IS NULL) AS questionCount, "
          + "qb.createdAt        AS createdAt, "
          + "qb.updatedAt        AS updatedAt "
          + "FROM QuestionBank qb JOIN User u ON u.id = qb.teacherId "
          + "WHERE qb.deletedAt IS NULL "
          + "AND (qb.teacherId = :teacherId OR qb.isPublic = true) "
          + "AND (:subject IS NULL OR qb.subject = :subject) "
          + "AND (:gradeLevel IS NULL OR qb.gradeLevel = :gradeLevel) "
          + "AND (:isPublic IS NULL OR qb.isPublic = :isPublic) "
          + "AND (:searchTerm IS NULL OR LOWER(qb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  Page<QuestionBankSummaryProjection> findSummaryWithFilters(
      @Param("teacherId") UUID teacherId,
      @Param("subject") String subject,
      @Param("gradeLevel") String gradeLevel,
      @Param("isPublic") Boolean isPublic,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);
}
