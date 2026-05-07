package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.enums.LessonStatus;
import java.util.Collection;
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
      "SELECT l FROM Lesson l WHERE l.chapterId = :chapterId AND l.deletedAt IS NULL"
          + " ORDER BY l.orderIndex, l.createdAt")
  List<Lesson> findByChapterIdAndNotDeleted(@Param("chapterId") UUID chapterId);

  @Query(
      "SELECT l FROM Lesson l WHERE l.chapterId = :chapterId"
          + " ORDER BY CASE WHEN l.deletedAt IS NULL THEN 0 ELSE 1 END ASC, l.orderIndex, l.createdAt")
  List<Lesson> findByChapterIdIncludingDeleted(@Param("chapterId") UUID chapterId);

  @Query(
      "SELECT l FROM Lesson l WHERE l.chapterId = :chapterId AND l.deletedAt IS NULL "
          + "AND LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%')) "
          + "ORDER BY l.orderIndex, l.createdAt")
  List<Lesson> findByChapterIdAndTitleContainingAndNotDeleted(
      @Param("chapterId") UUID chapterId, @Param("title") String title);

  @Query("SELECT COUNT(l) FROM Lesson l WHERE l.chapterId = :chapterId AND l.deletedAt IS NULL")
  Long countByChapterIdAndNotDeleted(@Param("chapterId") UUID chapterId);

  @Query(
      "SELECT l FROM Lesson l WHERE l.status = :status AND l.deletedAt IS NULL"
          + " ORDER BY l.orderIndex, l.createdAt")
  List<Lesson> findByStatusAndNotDeleted(@Param("status") LessonStatus status);

  @Query(
      "SELECT l FROM Lesson l WHERE l.status = :status AND l.deletedAt IS NULL "
          + "AND (l.createdBy = :teacherId OR l.updatedBy = :teacherId) "
          + "ORDER BY l.updatedAt DESC, l.createdAt DESC")
  List<Lesson> findTeacherSlideLessonsByStatus(
      @Param("teacherId") UUID teacherId, @Param("status") LessonStatus status);

  @Query(
      "SELECT l FROM Lesson l WHERE l.title = :title AND l.chapterId = :chapterId"
          + " AND l.deletedAt IS NULL")
  java.util.Optional<Lesson> findByTitleAndChapterIdAndNotDeleted(
      @Param("title") String title, @Param("chapterId") UUID chapterId);

  @Query("SELECT l FROM Lesson l WHERE l.id = :id AND l.deletedAt IS NULL")
  java.util.Optional<Lesson> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query("SELECT l.id FROM Lesson l WHERE l.id IN :ids AND l.deletedAt IS NULL")
  List<UUID> findExistingIdsByIds(@Param("ids") Collection<UUID> ids);

  @Query("SELECT l FROM Lesson l WHERE l.id IN :ids AND l.deletedAt IS NULL")
  List<Lesson> findByIdInAndNotDeleted(@Param("ids") Collection<UUID> ids);

  @Query("SELECT l FROM Lesson l WHERE l.chapterId IN :chapterIds AND l.deletedAt IS NULL ORDER BY l.orderIndex, l.createdAt")
  List<Lesson> findByChapterIdIn(@Param("chapterIds") List<UUID> chapterIds);
}
