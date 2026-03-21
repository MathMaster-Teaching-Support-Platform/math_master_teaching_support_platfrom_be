package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonPlan;
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
}
