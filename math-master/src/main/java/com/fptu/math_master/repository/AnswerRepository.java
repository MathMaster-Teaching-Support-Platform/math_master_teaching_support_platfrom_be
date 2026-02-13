package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    @Query("SELECT a FROM Answer a WHERE a.submissionId = :submissionId")
    List<Answer> findBySubmissionId(@Param("submissionId") UUID submissionId);

    @Query("SELECT a FROM Answer a WHERE a.submissionId = :submissionId AND a.questionId = :questionId")
    Optional<Answer> findBySubmissionIdAndQuestionId(
        @Param("submissionId") UUID submissionId,
        @Param("questionId") UUID questionId
    );

    @Query("SELECT COUNT(a) FROM Answer a WHERE a.submissionId = :submissionId")
    Long countBySubmissionId(@Param("submissionId") UUID submissionId);
}

