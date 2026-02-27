package com.fptu.math_master.repository;

import com.fptu.math_master.entity.AiReview;
import com.fptu.math_master.enums.AiReviewType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiReviewRepository extends JpaRepository<AiReview, UUID> {

  @Query("SELECT r FROM AiReview r WHERE r.submissionId = :submissionId ORDER BY r.createdAt DESC")
  List<AiReview> findBySubmissionId(@Param("submissionId") UUID submissionId);

  @Query(
      "SELECT r FROM AiReview r WHERE r.submissionId = :submissionId AND r.reviewType = :type ORDER BY r.createdAt DESC")
  List<AiReview> findBySubmissionIdAndType(
      @Param("submissionId") UUID submissionId, @Param("type") AiReviewType type);

  @Query("SELECT r FROM AiReview r WHERE r.answerId = :answerId ORDER BY r.createdAt DESC")
  List<AiReview> findByAnswerId(@Param("answerId") UUID answerId);

  @Query("SELECT COUNT(r) > 0 FROM AiReview r WHERE r.submissionId = :submissionId")
  boolean existsBySubmissionId(@Param("submissionId") UUID submissionId);
}

