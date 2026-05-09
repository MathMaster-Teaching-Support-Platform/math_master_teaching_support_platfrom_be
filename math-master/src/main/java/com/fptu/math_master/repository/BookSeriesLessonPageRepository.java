package com.fptu.math_master.repository;

import com.fptu.math_master.entity.BookSeriesLessonPage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookSeriesLessonPageRepository extends JpaRepository<BookSeriesLessonPage, UUID> {

  @Query(
      "SELECT m FROM BookSeriesLessonPage m WHERE m.bookSeriesId = :seriesId AND m.deletedAt IS NULL"
          + " ORDER BY m.orderIndex, m.pageStart")
  List<BookSeriesLessonPage> findBySeriesIdOrdered(@Param("seriesId") UUID seriesId);

  @Modifying
  @Query("DELETE FROM BookSeriesLessonPage m WHERE m.bookSeriesId = :seriesId")
  int hardDeleteAllBySeriesId(@Param("seriesId") UUID seriesId);
}

