package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Grade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

  @Query(
      "SELECT g FROM Grade g WHERE g.studentId = :studentId AND g.lesson.subject = :subject")
  List<Grade> findByStudentIdAndLessonSubject(
      @Param("studentId") UUID studentId, @Param("subject") String subject);

  @Query(
      "SELECT g FROM Grade g WHERE g.studentId = :studentId AND g.lessonId = :lessonId")
  List<Grade> findByStudentIdAndLessonId(
      @Param("studentId") UUID studentId, @Param("lessonId") UUID lessonId);

  @Query(
      "SELECT g FROM Grade g WHERE g.studentId = :studentId ORDER BY g.createdAt DESC")
  List<Grade> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") UUID studentId);
}
