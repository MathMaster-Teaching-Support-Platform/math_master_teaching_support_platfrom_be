package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "answers",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_answers", columnNames = {"submission_id", "question_id"})
  },
  indexes = {
    @Index(name = "idx_answers_submission", columnList = "submission_id"),
    @Index(name = "idx_answers_question", columnList = "question_id")
  }
)
public class Answer {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Lob
  @Nationalized
  @Column(name = "answer_text")
  private String answerText;

  @Type(JsonBinaryType.class)
  @Column(name = "answer_data", columnDefinition = "jsonb")
  private Map<String, Object> answerData;

  @Column(name = "is_correct")
  private Boolean isCorrect;

  @Column(name = "points_earned", precision = 5, scale = 2)
  private BigDecimal pointsEarned;

  @Lob
  @Nationalized
  @Column(name = "feedback")
  private String feedback;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

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

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

