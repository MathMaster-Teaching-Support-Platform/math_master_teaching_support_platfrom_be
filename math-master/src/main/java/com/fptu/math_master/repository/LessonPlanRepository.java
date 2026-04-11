package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, UUID> {

  @Query("SELECT lp FROM LessonPlan lp WHERE lp.id = :id AND lp.deletedAt IS NULL")
  Optional<LessonPlan> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT CASE WHEN COUNT(lp) > 0 THEN true ELSE false END FROM LessonPlan lp "
          + "WHERE lp.lessonId = :lessonId AND lp.teacherId = :teacherId AND lp.deletedAt IS NULL")
  boolean existsByLessonIdAndTeacherIdAndNotDeleted(
      @Param("lessonId") UUID lessonId, @Param("teacherId") UUID teacherId);

  @Query("SELECT lp FROM LessonPlan lp WHERE lp.lessonId = :lessonId AND lp.deletedAt IS NULL")
  List<LessonPlan> findByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);

  @Query(
      "SELECT lp FROM LessonPlan lp "
          + "JOIN Lesson l ON l.id = lp.lessonId "
          + "WHERE lp.lessonId = :lessonId "
          + "AND lp.deletedAt IS NULL "
          + "AND l.deletedAt IS NULL "
          + "AND LOWER(l.title) LIKE LOWER(CONCAT('%', :name, '%'))")
  List<LessonPlan> findByLessonIdAndLessonTitleContainingAndNotDeleted(
      @Param("lessonId") UUID lessonId, @Param("name") String name);

  @Query("SELECT lp FROM LessonPlan lp WHERE lp.teacherId = :teacherId AND lp.deletedAt IS NULL ORDER BY lp.createdAt DESC")
  List<LessonPlan> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId);

  @Query("SELECT lp FROM LessonPlan lp WHERE lp.teacherId = :teacherId AND lp.lessonId = :lessonId AND lp.deletedAt IS NULL")
  Optional<LessonPlan> findByTeacherIdAndLessonIdAndNotDeleted(
      @Param("teacherId") UUID teacherId, @Param("lessonId") UUID lessonId);

  @Query("SELECT COUNT(lp) FROM LessonPlan lp WHERE lp.deletedAt IS NULL")
  long countAllNotDeleted();
}
