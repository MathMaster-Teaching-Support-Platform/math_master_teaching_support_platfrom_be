package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CanonicalQuestion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CanonicalQuestionRepository extends JpaRepository<CanonicalQuestion, UUID> {

  @Query("SELECT c FROM CanonicalQuestion c WHERE c.id = :id AND c.deletedAt IS NULL")
  Optional<CanonicalQuestion> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT c FROM CanonicalQuestion c "
          + "WHERE c.createdBy = :createdBy AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
  Page<CanonicalQuestion> findByCreatedByAndNotDeleted(
      @Param("createdBy") UUID createdBy, Pageable pageable);
}