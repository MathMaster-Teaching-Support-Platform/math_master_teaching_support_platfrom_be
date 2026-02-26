package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Lesson;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository
    extends JpaRepository<Lesson, UUID>, JpaSpecificationExecutor<Lesson> {

  @Query(
      "SELECT l FROM Lesson l WHERE l.gradeLevel = :gradeLevel AND l.subject = :subject"
          + " AND l.deletedAt IS NULL ORDER BY l.createdAt")
  List<Lesson> findByGradeLevelAndSubjectAndNotDeleted(
      @Param("gradeLevel") String gradeLevel, @Param("subject") String subject);

  @Query(
      "SELECT COUNT(l) FROM Lesson l WHERE l.gradeLevel = :gradeLevel AND l.subject = :subject"
          + " AND l.title = :title AND l.deletedAt IS NULL")
  long countByGradeLevelAndSubjectAndTitle(
      @Param("gradeLevel") String gradeLevel,
      @Param("subject") String subject,
      @Param("title") String title);

  @Query(
      "SELECT l FROM Lesson l WHERE l.teacherId = :teacherId AND l.deletedAt IS NULL"
          + " ORDER BY l.createdAt DESC")
  List<Lesson> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId);

  @Query(
      "SELECT DISTINCT l FROM Lesson l INNER JOIN Chapter c ON l.id = c.lessonId "
          + "WHERE c.id = :chapterId AND l.gradeLevel = :gradeLevel AND l.deletedAt IS NULL")
  List<Lesson> findByChapterIdAndGradeLevel(
      @Param("chapterId") UUID chapterId, @Param("gradeLevel") String gradeLevel);
}
