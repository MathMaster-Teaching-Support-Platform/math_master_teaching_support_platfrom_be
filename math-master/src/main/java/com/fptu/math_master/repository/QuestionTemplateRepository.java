package com.fptu.math_master.repository;

import com.fptu.math_master.entity.QuestionTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {

  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator WHERE t.id = :id")
  Optional<QuestionTemplate> findByIdWithCreator(@Param("id") UUID id);

  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator")
  Page<QuestionTemplate> findAllWithCreator(Pageable pageable);
}
