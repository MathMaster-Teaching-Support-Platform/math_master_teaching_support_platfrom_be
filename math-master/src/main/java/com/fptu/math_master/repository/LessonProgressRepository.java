package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

  Optional<LessonProgress> findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
      UUID enrollmentId, UUID courseLessonId);

  @Query(
      "SELECT lp FROM LessonProgress lp "
          + "JOIN lp.courseLesson cl "
          + "WHERE lp.enrollmentId = :enrollmentId AND lp.deletedAt IS NULL AND cl.deletedAt IS NULL "
          + "ORDER BY cl.orderIndex ASC")
  List<LessonProgress> findByEnrollmentIdOrderByCourseLessonOrderIndex(
      @Param("enrollmentId") UUID enrollmentId);

  @Query(
      "SELECT COUNT(lp) FROM LessonProgress lp "
          + "JOIN lp.courseLesson cl "
          + "WHERE lp.enrollmentId = :enrollmentId AND lp.isCompleted = true "
          + "AND lp.deletedAt IS NULL AND cl.deletedAt IS NULL")
  long countCompletedByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);
}
