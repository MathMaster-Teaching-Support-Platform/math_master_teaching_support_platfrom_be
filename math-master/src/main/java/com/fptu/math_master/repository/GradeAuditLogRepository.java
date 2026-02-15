package com.fptu.math_master.repository;

import com.fptu.math_master.entity.GradeAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeAuditLogRepository extends JpaRepository<GradeAuditLog, UUID> {

  @Query(
      "SELECT g FROM GradeAuditLog g WHERE g.submissionId = :submissionId ORDER BY g.createdAt DESC")
  List<GradeAuditLog> findBySubmissionId(@Param("submissionId") UUID submissionId);

  @Query("SELECT g FROM GradeAuditLog g WHERE g.answerId = :answerId ORDER BY g.createdAt DESC")
  List<GradeAuditLog> findByAnswerId(@Param("answerId") UUID answerId);

  @Query("SELECT g FROM GradeAuditLog g WHERE g.teacherId = :teacherId ORDER BY g.createdAt DESC")
  List<GradeAuditLog> findByTeacherId(@Param("teacherId") UUID teacherId);
}
