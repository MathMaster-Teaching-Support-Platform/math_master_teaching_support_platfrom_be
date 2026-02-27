package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.QuestionDifficulty;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

  @Query(
      value =
          "SELECT * FROM questions q "
              + "WHERE q.chapter_id = :chapterId "
              + "AND q.difficulty = CAST(:difficulty AS question_difficulty) "
              + "AND :cognitiveLevel = ANY(q.bloom_taxonomy_tags) "
              + "AND (:questionType IS NULL OR q.question_type = CAST(:questionType AS question_type)) "
              + "AND q.deleted_at IS NULL "
              + "AND q.id NOT IN :excludedIds "
              + "ORDER BY RANDOM()",
      nativeQuery = true)
  List<Question> findSuggestedQuestions(
      @Param("chapterId") UUID chapterId,
      @Param("difficulty") String difficulty,
      @Param("cognitiveLevel") String cognitiveLevel,
      @Param("questionType") String questionType,
      @Param("excludedIds") List<UUID> excludedIds);

  @Query(
      "SELECT q FROM Question q WHERE q.chapterId = :chapterId "
          + "AND q.difficulty = :difficulty AND q.deletedAt IS NULL "
          + "ORDER BY q.createdAt DESC")
  List<Question> findByChapterIdAndDifficultyOrderByCreatedAt(
      @Param("chapterId") UUID chapterId, @Param("difficulty") QuestionDifficulty difficulty);
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
}
