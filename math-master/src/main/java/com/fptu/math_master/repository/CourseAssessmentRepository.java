package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CourseAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, UUID> {

  Optional<CourseAssessment> findByIdAndDeletedAtIsNull(UUID id);

  @Query(
      "SELECT ca FROM CourseAssessment ca "
          + "WHERE ca.courseId = :courseId AND ca.deletedAt IS NULL "
          + "ORDER BY ca.orderIndex ASC NULLS LAST, ca.createdAt ASC")
  List<CourseAssessment> findByCourseIdAndNotDeleted(@Param("courseId") UUID courseId);

  @Query(
      "SELECT ca FROM CourseAssessment ca "
          + "WHERE ca.assessmentId = :assessmentId AND ca.deletedAt IS NULL")
  List<CourseAssessment> findByAssessmentIdAndNotDeleted(@Param("assessmentId") UUID assessmentId);

  @Query(
      "SELECT COUNT(ca) FROM CourseAssessment ca "
          + "WHERE ca.courseId = :courseId AND ca.deletedAt IS NULL")
  long countByCourseIdAndNotDeleted(@Param("courseId") UUID courseId);

  @Query(
      "SELECT CASE WHEN COUNT(ca) > 0 THEN true ELSE false END "
          + "FROM CourseAssessment ca "
          + "WHERE ca.courseId = :courseId AND ca.assessmentId = :assessmentId AND ca.deletedAt IS NULL")
  boolean existsByCourseIdAndAssessmentIdAndNotDeleted(
      @Param("courseId") UUID courseId, @Param("assessmentId") UUID assessmentId);

  @Query(
      "SELECT ca FROM CourseAssessment ca "
          + "WHERE ca.courseId = :courseId AND ca.assessmentId = :assessmentId AND ca.deletedAt IS NULL")
  Optional<CourseAssessment> findByCourseIdAndAssessmentIdAndNotDeleted(
      @Param("courseId") UUID courseId, @Param("assessmentId") UUID assessmentId);

  void deleteByCourseId(UUID courseId);
}
