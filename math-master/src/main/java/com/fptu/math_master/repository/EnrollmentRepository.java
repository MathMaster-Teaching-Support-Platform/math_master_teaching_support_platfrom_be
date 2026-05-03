package com.fptu.math_master.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.enums.EnrollmentStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  Optional<Enrollment> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Enrollment> findByStudentIdAndCourseIdAndDeletedAtIsNull(UUID studentId, UUID courseId);

  // FIX #3: Add pessimistic locking to prevent concurrent enrollment duplicates
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.courseId = :courseId AND e.deletedAt IS NULL")
  Optional<Enrollment> findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(
      @Param("studentId") UUID studentId, 
      @Param("courseId") UUID courseId);

  List<Enrollment> findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(UUID studentId);

    @Query(
            "SELECT e FROM Enrollment e "
                    + "JOIN FETCH e.course c "
                    + "LEFT JOIN FETCH c.subject "
                    + "WHERE e.studentId = :studentId "
                    + "AND e.status = :status "
                    + "AND e.deletedAt IS NULL "
                    + "AND c.deletedAt IS NULL")
    List<Enrollment> findByStudentIdAndStatusWithCourseAndSubject(
            @Param("studentId") UUID studentId, @Param("status") EnrollmentStatus status);

    List<Enrollment> findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
            UUID studentId, EnrollmentStatus status);

    long countByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, EnrollmentStatus status);

  Page<Enrollment> findByCourseIdAndStatusAndDeletedAtIsNull(
      UUID courseId, EnrollmentStatus status, Pageable pageable);

  @Query(
      "SELECT COUNT(e) FROM Enrollment e "
          + "WHERE e.courseId = :courseId AND e.status = 'ACTIVE' AND e.deletedAt IS NULL")
  long countActiveEnrollmentsByCourseId(@Param("courseId") UUID courseId);

  long countByStatusAndDeletedAtIsNull(EnrollmentStatus status);

  long countByStatusAndDeletedAtIsNullAndCreatedAtBetween(
      EnrollmentStatus status, java.time.Instant from, java.time.Instant to);

  @Query("SELECT COUNT(DISTINCT e.studentId) FROM Enrollment e JOIN Course c ON c.id = e.courseId " +
         "WHERE c.teacherId = :teacherId AND e.status = 'ACTIVE' AND e.deletedAt IS NULL AND c.deletedAt IS NULL")
  int countStudentsByTeacherId(@Param("teacherId") UUID teacherId);

  List<Enrollment> findByCourseIdAndDeletedAtIsNull(UUID courseId);

  long countByCourseIdAndDeletedAtIsNull(UUID courseId);

  long countByStatusAndUpdatedAtBetween(EnrollmentStatus status, java.time.Instant start, java.time.Instant end);
}