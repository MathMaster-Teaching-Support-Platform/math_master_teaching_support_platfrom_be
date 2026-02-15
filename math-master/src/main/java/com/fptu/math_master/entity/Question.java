package com.fptu.math_master.entity;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
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
    name = "questions",
    indexes = {
      @Index(name = "idx_questions_bank", columnList = "question_bank_id"),
      @Index(name = "idx_questions_chapter", columnList = "chapter_id"),
      @Index(name = "idx_questions_created_by", columnList = "created_by"),
      @Index(name = "idx_questions_type", columnList = "question_type"),
      @Index(name = "idx_questions_difficulty", columnList = "difficulty"),
      @Index(name = "idx_questions_cognitive_level", columnList = "cognitive_level")
    })
public class Question {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "question_bank_id")
  private UUID questionBankId;

  @Column(name = "chapter_id")
  private UUID chapterId;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "question_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType questionType;

  @Lob
  @Nationalized
  @Column(name = "question_text", nullable = false)
  private String questionText;

  @Type(JsonBinaryType.class)
  @Column(name = "options", columnDefinition = "jsonb")
  private Map<String, Object> options;

  @Lob
  @Nationalized
  @Column(name = "correct_answer")
  private String correctAnswer;

  @Lob
  @Nationalized
  @Column(name = "explanation")
  private String explanation;

  @Column(name = "points", precision = 5, scale = 2)
  private BigDecimal points;

  @Column(name = "difficulty")
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty;

  @Size(max = 50)
  @Column(name = "cognitive_level", length = 50)
  private String cognitiveLevel;

  @Type(StringArrayType.class)
  @Column(name = "bloom_taxonomy_tags", columnDefinition = "TEXT[]")
  private String[] bloomTaxonomyTags;

  @Type(StringArrayType.class)
  @Column(name = "learning_objectives", columnDefinition = "TEXT[]")
  private String[] learningObjectives;

  @Type(StringArrayType.class)
  @Column(name = "tags", columnDefinition = "TEXT[]")
  private String[] tags;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_bank_id", insertable = false, updatable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Answer> answers;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MatrixQuestionMapping> matrixQuestionMappings;

  @PrePersist
  public void prePersist() {
    if (points == null) points = BigDecimal.valueOf(1.0);
    if (difficulty == null) difficulty = QuestionDifficulty.MEDIUM;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
