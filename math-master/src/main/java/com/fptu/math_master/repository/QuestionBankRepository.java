package com.fptu.math_master.repository;

import com.fptu.math_master.entity.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID>, JpaSpecificationExecutor<QuestionBank> {

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.id = :id AND qb.deletedAt IS NULL")
  Optional<QuestionBank> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.teacherId = :teacherId AND qb.deletedAt IS NULL")
  Page<QuestionBank> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId, Pageable pageable);

  @Query("SELECT qb FROM QuestionBank qb WHERE qb.isPublic = true AND qb.deletedAt IS NULL")
  Page<QuestionBank> findPublicQuestionBanks(Pageable pageable);

  @Query("SELECT qb FROM QuestionBank qb " +
         "WHERE qb.deletedAt IS NULL " +
         "AND (qb.teacherId = :teacherId OR qb.isPublic = true) " +
         "AND (:subject IS NULL OR qb.subject = :subject) " +
         "AND (:gradeLevel IS NULL OR qb.gradeLevel = :gradeLevel) " +
         "AND (:isPublic IS NULL OR qb.isPublic = :isPublic) " +
         "AND (:searchTerm IS NULL OR LOWER(qb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  Page<QuestionBank> findWithFilters(
    @Param("teacherId") UUID teacherId,
    @Param("subject") String subject,
    @Param("gradeLevel") String gradeLevel,
    @Param("isPublic") Boolean isPublic,
    @Param("searchTerm") String searchTerm,
    Pageable pageable
  );

  @Query("SELECT COUNT(q) FROM Question q WHERE q.questionBankId = :questionBankId AND q.deletedAt IS NULL")
  Long countQuestionsByQuestionBankId(@Param("questionBankId") UUID questionBankId);

  @Query("SELECT COUNT(aq) > 0 FROM AssessmentQuestion aq " +
         "JOIN Question q ON aq.questionId = q.id " +
         "WHERE q.questionBankId = :questionBankId AND q.deletedAt IS NULL")
  boolean hasQuestionsInUse(@Param("questionBankId") UUID questionBankId);
}

