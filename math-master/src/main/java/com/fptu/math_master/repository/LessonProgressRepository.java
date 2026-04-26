package com.fptu.math_master.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.LessonProgress;

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

  @Query(
      "SELECT lp.enrollmentId, COUNT(lp) FROM LessonProgress lp "
          + "JOIN lp.courseLesson cl "
          + "WHERE lp.enrollmentId IN :enrollmentIds "
          + "AND lp.isCompleted = true "
          + "AND lp.deletedAt IS NULL "
          + "AND cl.deletedAt IS NULL "
          + "GROUP BY lp.enrollmentId")
  List<Object[]> countCompletedByEnrollmentIds(@Param("enrollmentIds") List<UUID> enrollmentIds);

  @Query(
      "SELECT lp.lastWatchedAt, COALESCE(lp.watchedSeconds, 0) FROM LessonProgress lp "
          + "JOIN lp.enrollment e "
          + "WHERE e.studentId = :studentId "
          + "AND e.deletedAt IS NULL "
          + "AND lp.deletedAt IS NULL "
          + "AND lp.lastWatchedAt IS NOT NULL "
          + "AND lp.lastWatchedAt >= :from "
          + "AND lp.lastWatchedAt < :to")
  List<Object[]> findWatchActivityForWindow(
      @Param("studentId") UUID studentId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(
      "SELECT lp.lastWatchedAt FROM LessonProgress lp "
          + "JOIN lp.enrollment e "
          + "WHERE e.studentId = :studentId "
          + "AND e.deletedAt IS NULL "
          + "AND lp.deletedAt IS NULL "
          + "AND lp.lastWatchedAt IS NOT NULL "
          + "AND lp.lastWatchedAt >= :from")
  List<Instant> findLastWatchedAtAfter(
      @Param("studentId") UUID studentId,
      @Param("from") Instant from);
}
