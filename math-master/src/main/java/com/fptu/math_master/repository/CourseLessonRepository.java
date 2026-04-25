package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CourseLesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, UUID> {

  Optional<CourseLesson> findByIdAndDeletedAtIsNull(UUID id);

  @Query(
      "SELECT cl FROM CourseLesson cl "
          + "LEFT JOIN cl.lesson l "
          + "WHERE cl.courseId = :courseId AND cl.deletedAt IS NULL AND (l IS NULL OR l.deletedAt IS NULL) "
          + "ORDER BY cl.orderIndex ASC, cl.createdAt ASC")
  List<CourseLesson> findByCourseIdAndNotDeleted(@Param("courseId") UUID courseId);

  @Query(
      "SELECT COUNT(cl) FROM CourseLesson cl "
          + "LEFT JOIN cl.lesson l "
          + "WHERE cl.courseId = :courseId AND cl.deletedAt IS NULL AND (l IS NULL OR l.deletedAt IS NULL)")
  long countByCourseIdAndNotDeleted(@Param("courseId") UUID courseId);

  @Query(
      "SELECT cl.courseId, COUNT(cl) FROM CourseLesson cl "
          + "LEFT JOIN cl.lesson l "
          + "WHERE cl.courseId IN :courseIds "
          + "AND cl.deletedAt IS NULL "
          + "AND (l IS NULL OR l.deletedAt IS NULL) "
          + "GROUP BY cl.courseId")
  List<Object[]> countByCourseIdsAndNotDeleted(@Param("courseIds") List<UUID> courseIds);

  boolean existsByIdAndCourseIdAndDeletedAtIsNull(UUID id, UUID courseId);
}
