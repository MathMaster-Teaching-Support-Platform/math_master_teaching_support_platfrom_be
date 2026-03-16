package com.fptu.math_master.repository;

import com.fptu.math_master.entity.SlideTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SlideTemplateRepository extends JpaRepository<SlideTemplate, UUID> {

  @Query("SELECT st FROM SlideTemplate st WHERE st.id = :id AND st.deletedAt IS NULL")
  Optional<SlideTemplate> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT st FROM SlideTemplate st WHERE st.deletedAt IS NULL ORDER BY st.createdAt DESC")
  List<SlideTemplate> findAllNotDeleted();

  @Query(
      "SELECT st FROM SlideTemplate st WHERE st.deletedAt IS NULL AND st.isActive = true ORDER BY st.createdAt DESC")
  List<SlideTemplate> findAllActiveNotDeleted();
}
