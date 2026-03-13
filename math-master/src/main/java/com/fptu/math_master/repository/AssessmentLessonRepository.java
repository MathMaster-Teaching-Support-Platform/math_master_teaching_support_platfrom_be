package com.fptu.math_master.repository;

import com.fptu.math_master.entity.AssessmentLesson;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentLessonRepository extends JpaRepository<AssessmentLesson, UUID> {

  @Query(
      "SELECT al FROM AssessmentLesson al WHERE al.assessmentId = :assessmentId ORDER BY al.createdAt")
  List<AssessmentLesson> findByAssessmentIdOrderByCreatedAt(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT al.lessonId FROM AssessmentLesson al WHERE al.assessmentId = :assessmentId")
  List<UUID> findLessonIdsByAssessmentId(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT al FROM AssessmentLesson al WHERE al.assessmentId IN :assessmentIds")
  List<AssessmentLesson> findByAssessmentIdIn(@Param("assessmentIds") Collection<UUID> assessmentIds);

  void deleteByAssessmentId(UUID assessmentId);
}

