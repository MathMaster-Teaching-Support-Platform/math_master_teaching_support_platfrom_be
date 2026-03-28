package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.enums.EnrollmentStatus;
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
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  Optional<Enrollment> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Enrollment> findByStudentIdAndCourseIdAndDeletedAtIsNull(UUID studentId, UUID courseId);

  List<Enrollment> findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(UUID studentId);

  Page<Enrollment> findByCourseIdAndStatusAndDeletedAtIsNull(
      UUID courseId, EnrollmentStatus status, Pageable pageable);

  @Query(
      "SELECT COUNT(e) FROM Enrollment e "
          + "WHERE e.courseId = :courseId AND e.status = 'ACTIVE' AND e.deletedAt IS NULL")
  long countActiveEnrollmentsByCourseId(@Param("courseId") UUID courseId);
}
