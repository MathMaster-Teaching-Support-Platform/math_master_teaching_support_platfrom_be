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

import com.fptu.math_master.enums.TemplateStatus;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {

  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator WHERE t.id = :id")
  Optional<QuestionTemplate> findByIdWithCreator(@Param("id") UUID id);

    @Query(
            "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
                    + "WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<QuestionTemplate> findByIdWithCreatorAndNotDeleted(@Param("id") UUID id);

  // Kept for backward-compat with any callers that still use the unfiltered page
  @Query("SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator")
  Page<QuestionTemplate> findAllWithCreator(Pageable pageable);

  /**
   * Fix #2: fetch only the calling teacher's own non-deleted templates.
   * Replaces the broken getMyQuestionTemplates that used findAllWithCreator.
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL AND t.createdBy = :createdBy")
  Page<QuestionTemplate> findByCreatedByAndNotDeleted(
      @Param("createdBy") UUID createdBy, Pageable pageable);

  /**
   * Teacher's own templates with optional search term and status filter.
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL AND t.createdBy = :createdBy "
          + "AND (:status IS NULL OR t.status = :status) "
          + "AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search as string), '%')) "
          + "     OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', CAST(:search as string), '%')))")
  Page<QuestionTemplate> findByCreatedByWithSearch(
      @Param("createdBy") UUID createdBy,
      @Param("status") TemplateStatus status,
      @Param("search") String search,
      Pageable pageable);

  /**
   * Fix #3: search query with all filters properly applied.
   * Returns templates owned by the caller OR public templates.
   * Used by searchQuestionTemplates — was previously ignoring every param.
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL "
          + "AND (t.createdBy = :currentUserId OR t.isPublic = true) "
          + "AND (:isPublic IS NULL OR t.isPublic = :isPublic) "
          + "AND (:templateType IS NULL OR t.templateType = :templateType) "
          + "AND (:cognitiveLevel IS NULL OR t.cognitiveLevel = :cognitiveLevel) "
          + "AND (:searchTerm IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm as string), '%')))")
  Page<QuestionTemplate> searchTemplates(
      @Param("currentUserId") UUID currentUserId,
      @Param("isPublic") Boolean isPublic,
      @Param("templateType") QuestionType templateType,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Find active (non-deleted) templates visible to a teacher: owned by the teacher OR public,
   * with optional type/level/keyword filtering. Used by ExamMatrixServiceImpl cell matching.
   */
  @Query(
      "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
          + "WHERE t.deletedAt IS NULL "
          + "AND (:onlyMine = true  OR t.isPublic = true OR t.createdBy = :currentUserId) "
          + "AND (:publicOnly = false OR t.isPublic = true) "
          + "AND (:currentUserId IS NULL OR :onlyMine = false OR t.createdBy = :currentUserId) "
          + "AND (:templateType IS NULL OR t.templateType = :templateType) "
          + "AND (:cognitiveLevel IS NULL OR t.cognitiveLevel = :cognitiveLevel) "
          + "AND (:searchTerm IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm as string), '%')))")
  Page<QuestionTemplate> findMatchingTemplatesForCell(
      @Param("currentUserId") UUID currentUserId,
      @Param("onlyMine") boolean onlyMine,
      @Param("publicOnly") boolean publicOnly,
      @Param("templateType") QuestionType templateType,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Find all active templates owned by a user or public, without pagination.
   * Used internally by ExamMatrixServiceImpl for tag-based filtering and ranking.
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

  /**
   * Find all active question templates for a specific lesson.
   * Used by roadmap service to fetch templates associated with a lesson.
   */
  @Query("SELECT t FROM QuestionTemplate t WHERE t.lessonId = :lessonId AND t.deletedAt IS NULL")
  List<QuestionTemplate> findByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);

    @Query(
            "SELECT t FROM QuestionTemplate t "
                    + "WHERE t.questionBankId = :bankId "
                    + "AND t.cognitiveLevel = :cognitiveLevel "
                    + "AND t.status = 'PUBLISHED' "
                    + "AND t.deletedAt IS NULL "
                    + "ORDER BY t.createdAt")
    List<QuestionTemplate> findPublishedByBankAndCognitive(
            @Param("bankId") UUID bankId, @Param("cognitiveLevel") CognitiveLevel cognitiveLevel);

    @Query(
            "SELECT t FROM QuestionTemplate t LEFT JOIN FETCH t.creator "
                    + "WHERE t.questionBankId = :bankId AND t.deletedAt IS NULL "
                    + "ORDER BY t.createdAt DESC")
    List<QuestionTemplate> findByQuestionBankIdAndNotDeleted(@Param("bankId") UUID bankId);
}
