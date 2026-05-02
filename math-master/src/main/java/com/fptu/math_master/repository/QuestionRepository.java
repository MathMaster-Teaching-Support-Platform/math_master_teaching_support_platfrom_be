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
              + "  ))",
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

  // ========================================================================
  // Phase 2: Chapter-Based Query Methods
  // ========================================================================

  /**
   * Count approved questions by bank, chapter, and cognitive level.
   * This is the core query for the new chapter-based generation engine.
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL",
      nativeQuery = true)
  long countApprovedByBankAndChapterAndCognitive(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel);

  /**
   * Find approved question IDs by bank, chapter, and cognitive level.
   * Returns IDs in deterministic order for reproducible selection.
   */
  @Query(
      value =
          "SELECT q.id FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "ORDER BY q.id",
      nativeQuery = true)
  List<UUID> findApprovedIdsByBankAndChapterAndCognitive(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel);

  /**
   * Find random approved questions by bank, chapter, and cognitive level.
   * Used for quick random selection without pre-loading all IDs.
   */
  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "ORDER BY random() LIMIT :limit",
      nativeQuery = true)
  List<Question> findRandomApprovedByBankAndChapterAndCognitive(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("limit") int limit);

  // ========================================================================
  // Phase 5: Type-Aware Query Methods (MCQ, TRUE_FALSE, SHORT_ANSWER)
  // ========================================================================

  /**
   * Count approved questions by bank, chapter, cognitive level, and question type.
   * This is the core query for type-aware generation engine.
   * Supports MCQ, TRUE_FALSE, and SHORT_ANSWER question types.
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_type = CAST(:questionType AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL",
      nativeQuery = true)
  long countApprovedByBankAndChapterAndCognitiveAndType(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType);

  /**
   * Find approved question IDs by bank, chapter, cognitive level, and question type.
   * Returns IDs in deterministic order for reproducible selection.
   * Used by generation engine to select specific question types.
   */
  @Query(
      value =
          "SELECT q.id FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_type = CAST(:questionType AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "ORDER BY q.id",
      nativeQuery = true)
  List<UUID> findApprovedIdsByBankAndChapterAndCognitiveAndType(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType);

  /**
   * Find random approved questions by bank, chapter, cognitive level, and question type.
   * Used for quick random selection without pre-loading all IDs.
   * Supports type-specific question selection for MCQ, TF, and SA.
   */
  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_type = CAST(:questionType AS text) "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "ORDER BY random() LIMIT :limit",
      nativeQuery = true)
  List<Question> findRandomApprovedByBankAndChapterAndCognitiveAndType(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType,
      @Param("limit") int limit);

  /**
   * Count approved questions by bank, cognitive level, and question type (no chapter filter).
   * Used for percentage-based assessment generation that respects part question types.
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_type = CAST(:questionType AS text)",
      nativeQuery = true)
  long countApprovedByBankAndCognitiveAndType(
      @Param("bankId") UUID bankId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType);

  /**
   * Find random approved questions by bank, cognitive level, and question type (no chapter filter).
   * Used for percentage-based assessment generation that respects part question types.
   */
  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.cognitive_level = CAST(:cognitiveLevel AS text) "
              + "AND q.question_type = CAST(:questionType AS text) "
              + "ORDER BY random() LIMIT :limit",
      nativeQuery = true)
  List<Question> findRandomApprovedByBankAndCognitiveAndType(
      @Param("bankId") UUID bankId,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType,
      @Param("limit") int limit);

  /**
   * Get hierarchical matrix statistics for a question bank.
   * Groups questions by: Grade Level > Chapter > Question Type > Cognitive Level
   * Returns: [gradeLevel, chapterId, chapterName, questionType, cognitiveLevel, count]
   */
  @Query(
      value =
          "SELECT "
              + "  sg.grade_level as gradeLevel, "
              + "  c.id as chapterId, "
              + "  c.title as chapterName, "
              + "  q.question_type as questionType, "
              + "  q.cognitive_level as cognitiveLevel, "
              + "  COUNT(q.id) as questionCount "
              + "FROM questions q "
              + "JOIN question_templates t ON q.template_id = t.id "
              + "JOIN chapters c ON t.chapter_id = c.id "
              + "JOIN subjects s ON c.subject_id = s.id "
              + "JOIN school_grades sg ON s.school_grade_id = sg.id "
              + "WHERE q.question_bank_id = :bankId "
              + "  AND q.question_status = 'APPROVED' "
              + "  AND q.deleted_at IS NULL "
              + "GROUP BY sg.grade_level, c.id, c.title, q.question_type, q.cognitive_level "
              + "ORDER BY sg.grade_level, c.order_index, q.question_type, q.cognitive_level",
      nativeQuery = true)
  List<Object[]> findMatrixStatsByBankId(@Param("bankId") UUID bankId);

  // ========================================================================
  // TF Clause-Level Queries (Finale2: JSONB expansion)
  // ========================================================================

  /**
   * Matrix stats for non-TF questions only (MCQ, SHORT_ANSWER, etc.).
   * Same as findMatrixStatsByBankId but excludes TRUE_FALSE.
   */
  @Query(
      value =
          "SELECT "
              + "  sg.grade_level as gradeLevel, "
              + "  c.id as chapterId, "
              + "  c.title as chapterName, "
              + "  q.question_type as questionType, "
              + "  q.cognitive_level as cognitiveLevel, "
              + "  COUNT(q.id) as questionCount "
              + "FROM questions q "
              + "JOIN question_templates t ON q.template_id = t.id "
              + "JOIN chapters c ON t.chapter_id = c.id "
              + "JOIN subjects s ON c.subject_id = s.id "
              + "JOIN school_grades sg ON s.school_grade_id = sg.id "
              + "WHERE q.question_bank_id = :bankId "
              + "  AND q.question_type != 'TRUE_FALSE' "
              + "  AND q.question_status = 'APPROVED' "
              + "  AND q.deleted_at IS NULL "
              + "GROUP BY sg.grade_level, c.id, c.title, q.question_type, q.cognitive_level "
              + "ORDER BY sg.grade_level, c.order_index, q.question_type, q.cognitive_level",
      nativeQuery = true)
  List<Object[]> findNonTFMatrixStatsByBankId(@Param("bankId") UUID bankId);

  /**
   * TF clause-level stats: expands generation_metadata->'tfClauses' into individual rows.
   * Each clause (A/B/C/D) with its own cognitiveLevel is counted separately.
   * Returns same column shape as findMatrixStatsByBankId.
   */
  @Query(
      value =
          "SELECT "
              + "  sg.grade_level as gradeLevel, "
              + "  c.id as chapterId, "
              + "  c.title as chapterName, "
              + "  'TRUE_FALSE' as questionType, "
              + "  clause_data.cognitive as cognitiveLevel, "
              + "  COUNT(*) as questionCount "
              + "FROM questions q "
              + "JOIN question_templates t ON q.template_id = t.id "
              + "JOIN chapters c ON t.chapter_id = c.id "
              + "JOIN subjects s ON c.subject_id = s.id "
              + "JOIN school_grades sg ON s.school_grade_id = sg.id, "
              + "LATERAL ( "
              + "  SELECT value->>'cognitiveLevel' as cognitive "
              + "  FROM jsonb_each(q.generation_metadata->'tfClauses') "
              + "  WHERE value->>'cognitiveLevel' IS NOT NULL "
              + ") clause_data "
              + "WHERE q.question_bank_id = :bankId "
              + "  AND q.question_type = 'TRUE_FALSE' "
              + "  AND q.question_status = 'APPROVED' "
              + "  AND q.deleted_at IS NULL "
              + "  AND q.generation_metadata IS NOT NULL "
              + "  AND q.generation_metadata->'tfClauses' IS NOT NULL "
              + "GROUP BY sg.grade_level, c.id, c.title, clause_data.cognitive "
              + "ORDER BY sg.grade_level, c.order_index, clause_data.cognitive",
      nativeQuery = true)
  List<Object[]> findTFClauseStatsByBankId(@Param("bankId") UUID bankId);

  /**
   * Count TF questions that have at least one clause matching the given cognitive level.
   * Used by QuestionSelectionService for TF-aware matrix validation.
   */
  @Query(
      value =
          "SELECT COUNT(DISTINCT q.id) FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.question_type = 'TRUE_FALSE' "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.generation_metadata IS NOT NULL "
              + "AND EXISTS ( "
              + "  SELECT 1 FROM jsonb_each(q.generation_metadata->'tfClauses') "
              + "  WHERE value->>'cognitiveLevel' = CAST(:cognitiveLevel AS text) "
              + ")",
      nativeQuery = true)
  long countTFByBankAndChapterAndClauseCognitive(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel);

  /**
   * Find TF question IDs that have at least one clause matching the given cognitive level.
   * Used by QuestionSelectionService for TF-aware matrix generation.
   */
  @Query(
      value =
          "SELECT DISTINCT q.id FROM questions q "
              + "WHERE q.question_bank_id = :bankId "
              + "AND q.chapter_id = :chapterId "
              + "AND q.question_type = 'TRUE_FALSE' "
              + "AND q.question_status = 'APPROVED' "
              + "AND q.deleted_at IS NULL "
              + "AND q.generation_metadata IS NOT NULL "
              + "AND EXISTS ( "
              + "  SELECT 1 FROM jsonb_each(q.generation_metadata->'tfClauses') "
              + "  WHERE value->>'cognitiveLevel' = CAST(:cognitiveLevel AS text) "
              + ") "
              + "ORDER BY q.id",
      nativeQuery = true)
  List<UUID> findTFIdsByBankAndChapterAndClauseCognitive(
      @Param("bankId") UUID bankId,
      @Param("chapterId") UUID chapterId,
      @Param("cognitiveLevel") String cognitiveLevel);
}

