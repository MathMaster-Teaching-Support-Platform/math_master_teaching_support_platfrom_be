package com.fptu.math_master.repository;

import com.fptu.math_master.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

  @Query("SELECT aq FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId ORDER BY aq.orderIndex")
  List<AssessmentQuestion> findByAssessmentIdOrderByOrderIndex(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT aq FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId AND aq.questionId = :questionId")
  Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(
    @Param("assessmentId") UUID assessmentId,
    @Param("questionId") UUID questionId
  );

  @Query("SELECT MAX(aq.orderIndex) FROM AssessmentQuestion aq WHERE aq.assessmentId = :assessmentId")
  Integer findMaxOrderIndex(@Param("assessmentId") UUID assessmentId);

  void deleteByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);
}

