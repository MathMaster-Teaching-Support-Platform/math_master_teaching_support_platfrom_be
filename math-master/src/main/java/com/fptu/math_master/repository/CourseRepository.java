package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Course;
import com.fptu.math_master.enums.CourseStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

  Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

  List<Course> findByTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID teacherId);

  List<Course> findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(UUID teacherId);

  @Query("SELECT c FROM Course c WHERE c.isPublished = true AND c.deletedAt IS NULL " +
         "AND (c.subjectId = :subjectId OR c.schoolGradeId = :gradeId) " +
         "AND c.id != :currentCourseId " +
         "ORDER BY c.rating DESC, c.createdAt DESC")
  Page<Course> findRelatedCourses(
      @Param("subjectId") UUID subjectId, 
      @Param("gradeId") UUID gradeId, 
      @Param("currentCourseId") UUID currentCourseId, 
      Pageable pageable);

  /**
   * Public listing — LEFT JOIN subject + schoolGrade so that CUSTOM courses
   * with no subject_id / school_grade_id still appear.
   */
  @Query(
      value =
          "SELECT c.* FROM courses c "
              + "LEFT JOIN subjects s ON s.id = c.subject_id "
              + "LEFT JOIN school_grades sg ON sg.id = c.school_grade_id "
              + "WHERE c.is_published = true AND c.deleted_at IS NULL "
              + "AND (c.subject_id IS NULL OR s.is_active = true) "
              + "AND (c.school_grade_id IS NULL OR sg.is_active = true) "
              + "AND (:schoolGradeId IS NULL OR c.school_grade_id = :schoolGradeId) "
              + "AND (:subjectId IS NULL OR c.subject_id = :subjectId) "
              + "AND (:keyword IS NULL OR c.title::text ILIKE CONCAT('%', :keyword, '%')) "
              + "ORDER BY c.created_at DESC",
      countQuery =
          "SELECT COUNT(*) FROM courses c "
              + "LEFT JOIN subjects s ON s.id = c.subject_id "
              + "LEFT JOIN school_grades sg ON sg.id = c.school_grade_id "
              + "WHERE c.is_published = true AND c.deleted_at IS NULL "
              + "AND (c.subject_id IS NULL OR s.is_active = true) "
              + "AND (c.school_grade_id IS NULL OR sg.is_active = true) "
              + "AND (:schoolGradeId IS NULL OR c.school_grade_id = :schoolGradeId) "
              + "AND (:subjectId IS NULL OR c.subject_id = :subjectId) "
              + "AND (:keyword IS NULL OR c.title::text ILIKE CONCAT('%', :keyword, '%'))",
      nativeQuery = true)
  Page<Course> findPublishedCoursesWithFilter(
      @Param("schoolGradeId") UUID schoolGradeId,
      @Param("subjectId") UUID subjectId,
      @Param("keyword") String keyword,
      Pageable pageable);

  // Admin search — includes unpublished, no subject/grade join requirement
  @Query(
      value = "SELECT c.* FROM courses c "
          + "WHERE c.deleted_at IS NULL "
          + "AND (:keyword IS NULL OR c.title::text ILIKE CONCAT('%', :keyword, '%')) "
          + "ORDER BY c.created_at DESC",
      countQuery = "SELECT COUNT(*) FROM courses c "
          + "WHERE c.deleted_at IS NULL "
          + "AND (:keyword IS NULL OR c.title::text ILIKE CONCAT('%', :keyword, '%'))",
      nativeQuery = true)
  Page<Course> searchAllCoursesForAdmin(
      @Param("keyword") String keyword,
      Pageable pageable);

    Page<Course> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(CourseStatus status, Pageable pageable);

  List<Course> findByTeacherIdAndDeletedAtIsNull(UUID teacherId);
}
