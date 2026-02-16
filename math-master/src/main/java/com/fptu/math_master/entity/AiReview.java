package com.fptu.math_master.entity;

import com.fptu.math_master.enums.AiReviewType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "ai_reviews",
    indexes = {
      @Index(name = "idx_ai_reviews_submission", columnList = "submission_id"),
      @Index(name = "idx_ai_reviews_answer", columnList = "answer_id"),
      @Index(name = "idx_ai_reviews_type", columnList = "review_type")
    })
public class AiReview {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "answer_id")
  private UUID answerId;

  @Column(name = "review_type")
  @Enumerated(EnumType.STRING)
  private AiReviewType reviewType;

  @Column(name = "ai_model", length = 100, nullable = false)
  private String aiModel;

  @Lob
  @Nationalized
  @Column(name = "review_content", nullable = false)
  private String reviewContent;

  @Type(StringArrayType.class)
  @Column(name = "suggestions", columnDefinition = "TEXT[]")
  private String[] suggestions;

  @Column(name = "confidence_score", precision = 3, scale = 2)
  private BigDecimal confidenceScore;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id", insertable = false, updatable = false)
  private Answer answer;

  @PrePersist
  public void prePersist() {
    if (reviewType == null) reviewType = AiReviewType.OVERALL;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
