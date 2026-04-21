package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CourseReview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

  Page<CourseReview> findByCourseIdAndDeletedAtIsNull(UUID courseId, Pageable pageable);

  Page<CourseReview> findByCourseIdAndRatingAndDeletedAtIsNull(UUID courseId, Integer rating, Pageable pageable);

  Optional<CourseReview> findByCourseIdAndStudentIdAndDeletedAtIsNull(UUID courseId, UUID studentId);

  @Query("SELECT cr.rating as rating, COUNT(cr) as count FROM CourseReview cr " +
         "WHERE cr.courseId = :courseId AND cr.deletedAt IS NULL " +
         "GROUP BY cr.rating")
  java.util.List<Object[]> getRatingDistribution(@Param("courseId") UUID courseId);

  @Query("SELECT AVG(cr.rating) FROM CourseReview cr WHERE cr.courseId = :courseId AND cr.deletedAt IS NULL")
  Double calculateAverageRating(@Param("courseId") UUID courseId);

  long countByCourseIdAndDeletedAtIsNull(UUID courseId);

  @Query("SELECT AVG(cr.rating) FROM CourseReview cr JOIN Course c ON c.id = cr.courseId " +
         "WHERE c.teacherId = :teacherId AND cr.deletedAt IS NULL AND c.deletedAt IS NULL")
  Double calculateTeacherAverageRating(@Param("teacherId") UUID teacherId);

  @Query("SELECT COUNT(cr) FROM CourseReview cr JOIN Course c ON c.id = cr.courseId " +
         "WHERE c.teacherId = :teacherId AND cr.deletedAt IS NULL AND c.deletedAt IS NULL")
  long countByTeacherId(@Param("teacherId") UUID teacherId);
}
