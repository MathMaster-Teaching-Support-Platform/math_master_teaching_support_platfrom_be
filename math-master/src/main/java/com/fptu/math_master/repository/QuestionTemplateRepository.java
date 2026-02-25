package com.fptu.math_master.repository;

import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
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
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {

  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator WHERE t.id = :id")
  Optional<QuestionTemplate> findByIdWithCreator(@Param("id") UUID id);

  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator")
  Page<QuestionTemplate> findAllWithCreator(Pageable pageable);

  /**
   * Find active (non-deleted) templates visible to a teacher: - owned by the teacher, OR - public
   * templates (when onlyMine = false)
   *
   * <p>Additional optional filters: templateType, cognitiveLevel, name/tag search term. Excludes
   * soft-deleted templates (deletedAt IS NULL).
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL "
          + "AND (:onlyMine = true  OR t.isPublic = true OR t.createdBy = :currentUserId) "
          + "AND (:publicOnly = false OR t.isPublic = true) "
          + "AND (:currentUserId IS NULL OR :onlyMine = false OR t.createdBy = :currentUserId) "
          + "AND (:templateType IS NULL OR t.templateType = :templateType) "
          + "AND (:cognitiveLevel IS NULL OR t.cognitiveLevel = :cognitiveLevel) "
          + "AND (:searchTerm IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  Page<QuestionTemplate> findMatchingTemplatesForCell(
      @Param("currentUserId") UUID currentUserId,
      @Param("onlyMine") boolean onlyMine,
      @Param("publicOnly") boolean publicOnly,
      @Param("templateType") QuestionType templateType,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Find all active templates owned by a user or public, without pagination. Used internally for
   * tag-based filtering and ranking.
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL "
          + "AND (t.isPublic = true OR t.createdBy = :currentUserId) "
          + "AND (:templateType IS NULL OR t.templateType = :templateType) "
          + "AND (:cognitiveLevel IS NULL OR t.cognitiveLevel = :cognitiveLevel)")
  List<QuestionTemplate> findCandidateTemplates(
      @Param("currentUserId") UUID currentUserId,
      @Param("templateType") QuestionType templateType,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel);
}
