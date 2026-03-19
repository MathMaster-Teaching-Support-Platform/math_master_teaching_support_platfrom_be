package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import com.fptu.math_master.enums.AiReviewType;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "ai_reviews",
    indexes = {
      @Index(name = "idx_ai_reviews_submission", columnList = "submission_id"),
      @Index(name = "idx_ai_reviews_answer", columnList = "answer_id"),
      @Index(name = "idx_ai_reviews_type", columnList = "review_type")
    })
/**
 * The entity of 'AiReview'.
 */
public class AiReview extends BaseEntity {

  /**
   * submission
   */
  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  /**
   * answer
   */
  @Column(name = "answer_id", nullable = false)
  private UUID answerId;

  /**
   * review_type
   */
  @Builder.Default
  @Column(name = "review_type")
  @Enumerated(EnumType.STRING)
  private AiReviewType reviewType = AiReviewType.OVERALL;

  /**
   * ai_model
   */
  @Column(name = "ai_model", length = 100, nullable = false)
  private String aiModel;

  /**
   * review_content
   */
  @Nationalized
  @Column(name = "review_content", nullable = false)
  private String reviewContent;

  /**
   * suggestions
   */
  @Type(StringArrayType.class)
  @Column(name = "suggestions", columnDefinition = "TEXT[]")
  private String[] suggestions;

  /**
   * confidence_score
   */
  @Column(name = "confidence_score", precision = 3, scale = 2)
  private BigDecimal confidenceScore;

  /**
   * Relationships
   * - Many-to-One with Submission
   * - Many-to-One with Answer
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id", insertable = false, updatable = false)
  private Answer answer;
}
