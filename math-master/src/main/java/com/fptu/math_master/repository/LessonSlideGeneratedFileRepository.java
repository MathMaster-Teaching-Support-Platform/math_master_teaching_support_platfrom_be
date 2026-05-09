package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonSlideGeneratedFile;
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
public interface LessonSlideGeneratedFileRepository extends JpaRepository<LessonSlideGeneratedFile, UUID> {

  @Query("SELECT f FROM LessonSlideGeneratedFile f WHERE f.id = :id AND f.deletedAt IS NULL")
  Optional<LessonSlideGeneratedFile> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT f FROM LessonSlideGeneratedFile f "
          + "WHERE f.createdBy = :teacherId AND f.deletedAt IS NULL "
          + "ORDER BY f.createdAt DESC")
  List<LessonSlideGeneratedFile> findByTeacher(@Param("teacherId") UUID teacherId);

  @Query(
      "SELECT f FROM LessonSlideGeneratedFile f "
          + "LEFT JOIN Lesson l ON f.lessonId = l.id "
          + "LEFT JOIN Chapter c ON l.chapterId = c.id "
          + "LEFT JOIN Subject s ON c.subjectId = s.id "
          + "LEFT JOIN SchoolGrade g ON s.schoolGradeId = g.id "
          + "WHERE f.createdBy = :teacherId AND f.deletedAt IS NULL "
          + "AND (:lessonId IS NULL OR f.lessonId = :lessonId) "
          + "AND (:chapterId IS NULL OR c.id = :chapterId) "
          + "AND (:subjectId IS NULL OR s.id = :subjectId) "
          + "AND (:gradeId IS NULL OR g.id = :gradeId) "
          + "AND (:keyword IS NULL OR :keyword = '' "
          + "     OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "     OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "ORDER BY f.createdAt DESC")
  List<LessonSlideGeneratedFile> findByTeacherWithFilters(
      @Param("teacherId") UUID teacherId,
      @Param("gradeId") UUID gradeId,
      @Param("subjectId") UUID subjectId,
      @Param("chapterId") UUID chapterId,
      @Param("lessonId") UUID lessonId,
      @Param("keyword") String keyword);

  @Query(
      "SELECT f FROM LessonSlideGeneratedFile f "
          + "WHERE f.createdBy = :teacherId AND f.lessonId = :lessonId AND f.deletedAt IS NULL "
          + "ORDER BY f.createdAt DESC")
  List<LessonSlideGeneratedFile> findByTeacherAndLesson(
      @Param("teacherId") UUID teacherId, @Param("lessonId") UUID lessonId);

  @Query(
      "SELECT f FROM LessonSlideGeneratedFile f "
          + "WHERE f.id = :id AND f.isPublic = true AND f.deletedAt IS NULL")
  Optional<LessonSlideGeneratedFile> findPublicById(@Param("id") UUID id);

  @Query(
      "SELECT f FROM LessonSlideGeneratedFile f "
          + "WHERE f.lessonId = :lessonId AND f.isPublic = true AND f.deletedAt IS NULL "
          + "ORDER BY f.createdAt DESC")
  List<LessonSlideGeneratedFile> findPublicByLesson(@Param("lessonId") UUID lessonId);

    @Query(
            "SELECT f FROM LessonSlideGeneratedFile f "
                    + "WHERE f.isPublic = true AND f.deletedAt IS NULL "
                    + "ORDER BY f.createdAt DESC")
    List<LessonSlideGeneratedFile> findAllPublic();

      @Query(
          "SELECT f FROM LessonSlideGeneratedFile f "
              + "WHERE f.isPublic = true AND f.deletedAt IS NULL "
              + "AND (:gradeId IS NULL OR EXISTS ("
              + "    SELECT 1 FROM Lesson l "
              + "    JOIN Chapter c ON l.chapterId = c.id "
              + "    JOIN Subject s ON c.subjectId = s.id "
              + "    JOIN SchoolGrade g ON s.schoolGradeId = g.id "
              + "    WHERE l.id = f.lessonId AND g.id = :gradeId)) "
              + "AND (:subjectId IS NULL OR EXISTS ("
              + "    SELECT 1 FROM Lesson l "
              + "    JOIN Chapter c ON l.chapterId = c.id "
              + "    JOIN Subject s ON c.subjectId = s.id "
              + "    WHERE l.id = f.lessonId AND s.id = :subjectId)) "
              + "AND (:chapterId IS NULL OR EXISTS ("
              + "    SELECT 1 FROM Lesson l "
              + "    WHERE l.id = f.lessonId AND l.chapterId = :chapterId)) "
              + "AND (:lessonId IS NULL OR f.lessonId = :lessonId) "
              + "AND (:keyword IS NULL OR :keyword = '' "
              + "     OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
              + "     OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
      Page<LessonSlideGeneratedFile> findAllPublicWithFilters(
          @Param("gradeId") UUID gradeId,
          @Param("subjectId") UUID subjectId,
          @Param("chapterId") UUID chapterId,
          @Param("lessonId") UUID lessonId,
          @Param("keyword") String keyword,
          Pageable pageable);
}
