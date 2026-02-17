package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Question;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
