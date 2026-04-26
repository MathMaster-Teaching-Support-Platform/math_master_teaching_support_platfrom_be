package com.fptu.math_master.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.CourseAssessment;

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
      "SELECT ca FROM CourseAssessment ca "
          + "WHERE ca.courseId IN :courseIds AND ca.deletedAt IS NULL")
  List<CourseAssessment> findByCourseIdInAndNotDeleted(@Param("courseIds") Collection<UUID> courseIds);

  @Query(
      "SELECT a, COALESCE(s.name, c.title) FROM Enrollment e "
          + "JOIN e.course c "
          + "JOIN CourseAssessment ca ON ca.courseId = c.id AND ca.deletedAt IS NULL "
          + "JOIN Assessment a ON a.id = ca.assessmentId AND a.deletedAt IS NULL "
          + "LEFT JOIN Subject s ON s.id = c.subjectId "
          + "WHERE e.studentId = :studentId "
          + "AND e.status = com.fptu.math_master.enums.EnrollmentStatus.ACTIVE "
          + "AND e.deletedAt IS NULL "
          + "AND c.deletedAt IS NULL "
          + "AND a.status = com.fptu.math_master.enums.AssessmentStatus.PUBLISHED "
          + "AND (a.endDate IS NULL OR a.endDate >= :now) "
          + "ORDER BY a.endDate ASC NULLS LAST, a.createdAt DESC")
  List<Object[]> findUpcomingAssessmentsByStudentId(
      @Param("studentId") UUID studentId,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      "SELECT ca.assessmentId, COALESCE(s.name, c.title) FROM CourseAssessment ca "
          + "JOIN ca.course c "
          + "LEFT JOIN c.subject s "
          + "WHERE ca.assessmentId IN :assessmentIds "
          + "AND ca.deletedAt IS NULL "
          + "AND c.deletedAt IS NULL")
  List<Object[]> findSubjectNameByAssessmentIds(@Param("assessmentIds") Collection<UUID> assessmentIds);

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
