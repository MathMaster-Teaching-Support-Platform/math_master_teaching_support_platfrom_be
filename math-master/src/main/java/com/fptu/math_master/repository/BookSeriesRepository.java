package com.fptu.math_master.repository;

import com.fptu.math_master.entity.BookSeries;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookSeriesRepository extends JpaRepository<BookSeries, UUID> {

  @Query("SELECT s FROM BookSeries s WHERE s.id = :id AND s.deletedAt IS NULL")
  Optional<BookSeries> findByIdAndNotDeleted(@Param("id") UUID id);
}

