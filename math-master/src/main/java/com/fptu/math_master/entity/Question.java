package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.enums.QuestionType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "createdBy",
    column = @Column(name = "created_by", updatable = false, nullable = false))
@Table(
    name = "questions",
    indexes = {
      @Index(name = "idx_questions_bank", columnList = "question_bank_id"),
      @Index(name = "idx_questions_created_by", columnList = "created_by"),
      @Index(name = "idx_questions_type", columnList = "question_type"),
      @Index(name = "idx_questions_cognitive_level", columnList = "cognitive_level"),
      @Index(name = "idx_questions_status", columnList = "question_status"),
      @Index(name = "idx_questions_source_type", columnList = "question_source_type"),
      @Index(name = "idx_questions_template", columnList = "template_id")
    })
/**
 * The entity of 'Question'.
 */
public class Question extends BaseEntity {

  @Column(name = "question_bank_id")
  private UUID questionBankId;

  @Column(name = "chapter_id")
  private UUID chapterId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "question_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType questionType;

  @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
  private String questionText;

  @Type(JsonBinaryType.class)
  @Column(name = "options", columnDefinition = "jsonb")
  private Map<String, Object> options;

  @Column(name = "correct_answer", columnDefinition = "TEXT")
  private String correctAnswer;

  @Column(name = "explanation", columnDefinition = "TEXT")
  private String explanation;

  @Column(name = "points", precision = 5, scale = 2)
  private BigDecimal points;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Column(name = "question_status")
  @Enumerated(EnumType.STRING)
  private QuestionStatus questionStatus;

  @Column(name = "question_source_type")
  @Enumerated(EnumType.STRING)
  private QuestionSourceType questionSourceType;

  @Type(StringArrayType.class)
  @Column(name = "bloom_taxonomy_tags", columnDefinition = "TEXT[]")
  private String[] bloomTaxonomyTags;

  @Type(StringArrayType.class)
  @Column(name = "learning_objectives", columnDefinition = "TEXT[]")
  private String[] learningObjectives;

  @Type(StringArrayType.class)
  @Column(name = "tags", columnDefinition = "TEXT[]")
  private String[] tags;

  @Column(name = "template_id")
  private UUID templateId;

  @Column(name = "canonical_question_id")
  private UUID canonicalQuestionId;

  @Column(name = "solution_steps", columnDefinition = "TEXT")
  private String solutionSteps;

  @Column(name = "diagram_data", columnDefinition = "TEXT")
  private String diagramData;

  @Type(JsonBinaryType.class)
  @Column(name = "generation_metadata", columnDefinition = "jsonb")
  private Map<String, Object> generationMetadata;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_bank_id", insertable = false, updatable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", insertable = false, updatable = false)
  private QuestionTemplate questionTemplate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "canonical_question_id", insertable = false, updatable = false)
  private CanonicalQuestion canonicalQuestion;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Answer> answers;

  @PrePersist
  @Override
  public void prePersist() {
    super.prePersist();
    if (points == null) points = BigDecimal.valueOf(1.0);
    if (questionStatus == null) questionStatus = QuestionStatus.AI_DRAFT;
    if (questionSourceType == null) questionSourceType = QuestionSourceType.MANUAL;
  }
}
