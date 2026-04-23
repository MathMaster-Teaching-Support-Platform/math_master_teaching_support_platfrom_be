package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.enums.QuestionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

  /**
   * Detaches all questions that are NOT currently referenced by any assessment from the given bank.
   * Called during bank soft-deletion so that free questions are not silently orphaned behind a
   * deleted bank reference. Questions already used in an assessment are left untouched (deletion
   * is blocked upstream before this is reached).
   */
  @Modifying
  @Query(
      "UPDATE Question q SET q.questionBankId = NULL "
          + "WHERE q.questionBankId = :bankId "
          + "AND q.deletedAt IS NULL "
          + "AND q.id NOT IN ("
          + "  SELECT aq.questionId FROM AssessmentQuestion aq "
          + "  JOIN Question qq ON aq.questionId = qq.id "
          + "  WHERE qq.questionBankId = :bankId AND qq.deletedAt IS NULL"
          + ")")
  int detachFreeQuestionsFromBank(@Param("bankId") UUID bankId);

  @Query("SELECT q FROM Question q WHERE q.id = :id AND q.deletedAt IS NULL")
  java.util.Optional<Question> findByIdAndNotDeleted(@Param("id") UUID id);

    Optional<Question> findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            String renderedLatexHash);

  @Query(
      "SELECT q FROM Question q WHERE q.createdBy = :createdBy AND q.deletedAt IS NULL ORDER BY q.createdAt DESC")
  Page<Question> findByCreatedByAndNotDeleted(
      @Param("createdBy") UUID createdBy, Pageable pageable);

  @Query(
      nativeQuery = true,
      value =
          "SELECT q.* FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (CAST(:name AS text) IS NULL OR q.question_text ILIKE CAST(:name AS text)) "
              + "AND (CAST(:tag AS text) IS NULL OR EXISTS (SELECT 1 FROM unnest(q.tags) t WHERE t ILIKE CAST(:tag AS text))) "
              + "ORDER BY q.created_at DESC",
      countQuery =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (CAST(:name AS text) IS NULL OR q.question_text ILIKE CAST(:name AS text)) "
              + "AND (CAST(:tag AS text) IS NULL OR EXISTS (SELECT 1 FROM unnest(q.tags) t WHERE t ILIKE CAST(:tag AS text)))")
  Page<Question> findByCreatedByWithSearch(
      @Param("createdBy") UUID createdBy,
      @Param("name") String name,
      @Param("tag") String tag,
      Pageable pageable);

  /**
   * Full-text search by keyword (question_text ILIKE) and optional multi-tag filter.
   * tagsParam is a comma-separated list of tag values; any match satisfies the filter.
   */
  @Query(
      nativeQuery = true,
      value =
          "SELECT q.* FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (CAST(:keyword AS text) IS NULL OR q.question_text ILIKE CAST(:keyword AS text)) "
              + "AND (CAST(:tagsParam AS text) IS NULL "
              + "  OR EXISTS ( "
              + "    SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "    WHERE LOWER(t) = ANY(string_to_array(LOWER(CAST(:tagsParam AS text)), ',')) "
              + "  )) "
              + "ORDER BY q.created_at DESC",
      countQuery =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (CAST(:keyword AS text) IS NULL OR q.question_text ILIKE CAST(:keyword AS text)) "
              + "AND (CAST(:tagsParam AS text) IS NULL "
              + "  OR EXISTS ( "
              + "    SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "    WHERE LOWER(t) = ANY(string_to_array(LOWER(CAST(:tagsParam AS text)), ',')) "
              + "  ))")
  Page<Question> searchByKeywordAndTags(
      @Param("createdBy") UUID createdBy,
      @Param("keyword") String keyword,
      @Param("tagsParam") String tagsParam,
      Pageable pageable);

  @Query(
      nativeQuery = true,
      value =
          "SELECT q.* FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND q.id NOT IN ("
              + "  SELECT aq.question_id FROM assessment_questions aq WHERE aq.assessment_id = :assessmentId"
              + ") "
              + "AND (CAST(:keyword AS text) IS NULL OR q.question_text ILIKE CAST(:keyword AS text)) "
              + "AND (CAST(:tag AS text) IS NULL "
              + "  OR EXISTS ("
              + "    SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "    WHERE t ILIKE CAST(:tag AS text)"
              + "  )) "
              + "ORDER BY q.created_at DESC",
      countQuery =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND q.id NOT IN ("
              + "  SELECT aq.question_id FROM assessment_questions aq WHERE aq.assessment_id = :assessmentId"
              + ") "
              + "AND (CAST(:keyword AS text) IS NULL OR q.question_text ILIKE CAST(:keyword AS text)) "
              + "AND (CAST(:tag AS text) IS NULL "
              + "  OR EXISTS ("
              + "    SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "    WHERE t ILIKE CAST(:tag AS text)"
              + "  ))")
  Page<Question> findAvailableByAssessmentId(
      @Param("createdBy") UUID createdBy,
      @Param("assessmentId") UUID assessmentId,
      @Param("keyword") String keyword,
      @Param("tag") String tag,
      Pageable pageable);

  @Query(
      "SELECT q FROM Question q WHERE q.questionBankId = :bankId AND q.deletedAt IS NULL ORDER BY q.createdAt DESC")
  Page<Question> findByQuestionBankIdAndNotDeleted(@Param("bankId") UUID bankId, Pageable pageable);

  @Query(
      "SELECT q.cognitiveLevel, COUNT(q) FROM Question q "
          + "WHERE q.questionBankId = :bankId AND q.deletedAt IS NULL "
          + "GROUP BY q.cognitiveLevel")
  List<Object[]> countByCognitiveLevelForBank(@Param("bankId") UUID bankId);

  @Query(
      "SELECT q FROM Question q WHERE q.templateId = :templateId AND q.deletedAt IS NULL ORDER BY q.createdAt DESC")
  List<Question> findByTemplateIdAndNotDeleted(@Param("templateId") UUID templateId);

  @Query(
      "SELECT q FROM Question q WHERE q.canonicalQuestionId = :canonicalQuestionId AND q.deletedAt IS NULL ORDER BY q.createdAt DESC")
  Page<Question> findByCanonicalQuestionIdAndNotDeleted(
      @Param("canonicalQuestionId") UUID canonicalQuestionId, Pageable pageable);

  @Query(
      "SELECT q FROM Question q "
          + "WHERE q.templateId = :templateId "
          + "AND q.questionStatus = 'APPROVED' "
          + "AND q.deletedAt IS NULL "
          + "ORDER BY q.createdAt DESC")
  List<Question> findApprovedByTemplateIdAndNotDeleted(@Param("templateId") UUID templateId);

  @Query(
      nativeQuery = true,
      value =
          "SELECT q.* FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (q.question_text ILIKE :searchPattern "
              + "  OR q.explanation ILIKE :searchPattern)",
      countQuery =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.deleted_at IS NULL "
              + "AND q.created_by = :createdBy "
              + "AND (q.question_text ILIKE :searchPattern "
              + "  OR q.explanation ILIKE :searchPattern)")
  Page<Question> searchByCreatedBy(
      @Param("createdBy") UUID createdBy,
      @Param("searchPattern") String searchPattern,
      Pageable pageable);

  @Query(
      "SELECT q FROM Question q "
          + "WHERE q.deletedAt IS NULL "
          + "AND q.createdBy = :createdBy "
          + "AND (:questionType IS NULL OR q.questionType = :questionType) "
          + "ORDER BY q.createdAt DESC")
  Page<Question> findByFilters(
      @Param("createdBy") UUID createdBy,
      @Param("questionType") QuestionType questionType,
      Pageable pageable);

  @Modifying
  @Query(
      "UPDATE Question q SET q.deletedAt = CURRENT_TIMESTAMP WHERE q.id = :id AND q.deletedAt IS NULL")
  int softDeleteById(@Param("id") UUID id);

  @Query(
      "SELECT q FROM Question q "
          + "WHERE q.templateId = :templateId "
          + "AND q.questionBankId = :bankId "
          + "AND q.questionStatus = 'APPROVED' "
          + "AND q.deletedAt IS NULL "
          + "ORDER BY q.createdAt DESC")
  List<Question> findApprovedByTemplateIdAndQuestionBankIdAndNotDeleted(
      @Param("templateId") UUID templateId, @Param("bankId") UUID bankId);

  @Query(
      "SELECT q FROM Question q "
          + "WHERE q.id = :id "
          + "AND q.deletedAt IS NULL "
          + "AND q.questionStatus = :status")
  java.util.Optional<Question> findByIdAndStatusAndNotDeleted(
      @Param("id") UUID id, @Param("status") QuestionStatus status);

  @Query(
      "SELECT COUNT(q) FROM Question q "
          + "WHERE q.questionBankId = :bankId "
          + "AND q.questionStatus = 'APPROVED' "
          + "AND q.deletedAt IS NULL "
          + "AND (:difficulty IS NULL OR :difficulty IS NOT NULL) "
          + "AND (:cognitiveLevel IS NULL OR q.cognitiveLevel = :cognitiveLevel)")
  long countApprovedByBankAndDifficultyAndCognitive(
      @Param("bankId") UUID bankId,
      @Param("difficulty") QuestionDifficulty difficulty,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel);

  @Query(
      value =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND (:difficulty IS NULL OR :difficulty IS NOT NULL) "
              + "AND (:cognitiveLevel IS NULL OR q.cognitive_level = CAST(:cognitiveLevel AS text)) "
              + "AND (:topic IS NULL "
              + "  OR EXISTS ( "
              + "      SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "      WHERE lower(trim(t)) = lower(trim(CAST(:topic AS text))) "
              + "  ))",
      nativeQuery = true)
  long countApprovedByBankAndDifficultyAndCognitiveAndTopic(
      @Param("bankId") UUID bankId,
      @Param("difficulty") String difficulty,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("topic") String topic);

  @Query(
      value =
          "SELECT q.id FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND (:difficulty IS NULL OR :difficulty IS NOT NULL) "
              + "AND (:cognitiveLevel IS NULL OR q.cognitive_level = CAST(:cognitiveLevel AS text)) "
              + "AND (:topic IS NULL "
              + "  OR EXISTS ( "
              + "      SELECT 1 FROM unnest(COALESCE(q.tags, ARRAY[]::text[])) t "
              + "      WHERE lower(trim(t)) = lower(trim(CAST(:topic AS text))) "
              + "  )) "
              + "ORDER BY q.id",
      nativeQuery = true)
  List<UUID> findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
      @Param("bankId") UUID bankId,
      @Param("difficulty") String difficulty,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("topic") String topic);

  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND (:difficulty IS NULL OR :difficulty IS NOT NULL) "
              + "AND (:cognitiveLevel IS NULL OR q.cognitive_level = CAST(:cognitiveLevel AS text)) "
              + "ORDER BY random() LIMIT :limit",
      nativeQuery = true)
  List<Question> findRandomApprovedByBankAndDifficultyAndCognitive(
      @Param("bankId") UUID bankId,
      @Param("difficulty") String difficulty,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("limit") int limit);

  /**
   * Find random approved questions from multiple banks by cognitive level.
   * Used for percentage-based assessment generation from exam matrix.
   */
  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.question_bank_id IN (:bankIds) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "ORDER BY random() LIMIT :limit",
      nativeQuery = true)
  List<Question> findRandomApprovedByBanksAndCognitiveLevel(
      @Param("bankIds") List<UUID> bankIds,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("limit") int limit);

  /**
   * Count approved questions from multiple banks by cognitive level.
   * Used to check availability before generating percentage-based assessments.
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.question_bank_id IN (:bankIds) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text)",
      nativeQuery = true)
  long countApprovedByBanksAndCognitiveLevel(
      @Param("bankIds") List<UUID> bankIds,
      @Param("cognitiveLevel") String cognitiveLevel);
}
