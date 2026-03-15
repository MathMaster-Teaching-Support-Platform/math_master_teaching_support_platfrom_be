package com.fptu.math_master.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "answers",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_answers",
          columnNames = {"submission_id", "question_id"})
    },
    indexes = {
      @Index(name = "idx_answers_submission", columnList = "submission_id"),
      @Index(name = "idx_answers_question", columnList = "question_id")
    })
/**
 * The entity of 'Answer'.
 */
public class Answer extends BaseEntity {

  /**
   * submission_id
   */
  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  /**
   * question_id
   */
  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  /**
   * answer_text
   */
  @Lob
  @Nationalized
  @Column(name = "answer_text")
  private String answerText;

  /**
   * answer_data
   */
  @Type(JsonBinaryType.class)
  @Column(name = "answer_data", columnDefinition = "jsonb")
  private Map<String, Object> answerData;

  /**
   * is_correct
   */
  @Column(name = "is_correct")
  private Boolean isCorrect;

  /**
   * points_earned
   */
  @Column(name = "points_earned", precision = 5, scale = 2)
  private BigDecimal pointsEarned;

  /**
   * feedback
   */
  @Lob
  @Nationalized
  @Column(name = "feedback")
  private String feedback;

  /**
   * Relationships
   * - Many-to-One with Submission
   * - Many-to-One with Question
   * - One-to-Many with AiReview
   * - One-to-Many with GradeAuditLog
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AiReview> aiReviews;

  @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<GradeAuditLog> gradeAuditLogs;
}
