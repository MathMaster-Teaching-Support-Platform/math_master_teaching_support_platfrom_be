package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.enums.TemplateVariant;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    name = "question_templates",
    indexes = {
      @Index(name = "idx_question_templates_created_by", columnList = "created_by"),
      @Index(name = "idx_question_templates_lesson", columnList = "lesson_id"),
      @Index(name = "idx_question_templates_question_bank", columnList = "question_bank_id"),
      @Index(name = "idx_question_templates_tags", columnList = "tags"),
      @Index(name = "idx_question_templates_cognitive", columnList = "cognitive_level"),
      @Index(name = "idx_question_templates_variant", columnList = "template_variant"),
      @Index(name = "idx_question_templates_public", columnList = "is_public")
    })
/**
 * The entity of 'QuestionTemplate'.
 */
public class QuestionTemplate extends BaseEntity {

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "question_bank_id")
  private UUID questionBankId;

  @Column(name = "canonical_question_id")
  private UUID canonicalQuestionId;

  @Nationalized
  @Column(name = "name", nullable = false, columnDefinition = "TEXT")
  private String name;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "template_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType templateType;

  @Column(name = "template_variant")
  @Enumerated(EnumType.STRING)
  private TemplateVariant templateVariant;

  /**
   * Multi-language support for question template text Example: {"en": "What is {x} + {y}?", "vi":
   * "{x} + {y} bằng bao nhiêu?"}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "template_text", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> templateText;

  /**
   * Parameters that can be substituted in the template (PARAMETRIC variant)
   * Example: {"x": {"type": "integer", "min": 1, "max": 100}, "y": {"type": "integer", "min": 1, "max": 100}}
   * Nullable for PROPOSITIONAL templates
   */
  @Type(JsonBinaryType.class)
  @Column(name = "parameters", columnDefinition = "jsonb")
  private Map<String, Object> parameters;

  /** Formula to calculate the correct answer (PARAMETRIC variant) Example: "x + y" or "Math.sqrt(x^2 + y^2)" */
  @Column(name = "answer_formula", columnDefinition = "TEXT")
  private String answerFormula;

  @Column(name = "diagram_template", columnDefinition = "TEXT")
  private String diagramTemplate;

  /**
   * Configuration for generating multiple choice options (PARAMETRIC variant)
   * Example: {"type": "around_answer", "count": 4, "range": 10}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "options_generator", columnDefinition = "jsonb")
  private Map<String, Object> optionsGenerator;

  /** Constraints for parameter generation (PARAMETRIC variant) Example: ["x < y", "x + y < 1000"] */
  @Type(StringArrayType.class)
  @Column(name = "constraints", columnDefinition = "TEXT[]")
  private String[] constraints;

  /** Theorem statement for PROPOSITIONAL templates - defines the mathematical proposition */
  @Nationalized
  @Column(name = "theorem_statement")
  private String theoremStatement;

  /** Statement mutations for PROPOSITIONAL templates - variations of the theorem */
  @Type(JsonBinaryType.class)
  @Column(name = "statement_mutations", columnDefinition = "jsonb")
  private Map<String, Object> statementMutations;

  /** Question stem variations for both PARAMETRIC and PROPOSITIONAL templates */
  @Type(JsonBinaryType.class)
  @Column(name = "stem_variants", columnDefinition = "jsonb")
  private Map<String, Object> stemVariants;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Type(StringArrayType.class)
  @Column(name = "tags", nullable = false, columnDefinition = "TEXT[]")
  private String[] tags;

  @Builder.Default
  @Column(name = "is_public", nullable = false)
  private Boolean isPublic = false;

  @Builder.Default
  @Column(name = "usage_count", nullable = false)
  private Integer usageCount = 0;

  @Column(name = "avg_success_rate", precision = 5, scale = 2)
  private BigDecimal avgSuccessRate;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TemplateStatus status = TemplateStatus.DRAFT;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_bank_id", insertable = false, updatable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "canonical_question_id", insertable = false, updatable = false)
  private CanonicalQuestion canonicalQuestion;

  @OneToMany(mappedBy = "questionTemplate", cascade = CascadeType.ALL)
  private Set<Question> questions;

  @OneToMany(mappedBy = "questionTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> examMatrixMappings;

  @PrePersist
  @Override
  public void prePersist() {
    super.prePersist();
    if (isPublic == null) isPublic = false;
    if (usageCount == null) usageCount = 0;
    if (status == null) status = TemplateStatus.DRAFT;
  }

  /** Increment usage count when template is used to generate a question */
  public void incrementUsageCount() {
    this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
  }

  /** Update average success rate based on student performance */
  public void updateSuccessRate(BigDecimal newRate) {
    if (this.avgSuccessRate == null) {
      this.avgSuccessRate = newRate;
    } else {
      // Weighted average: (current * usageCount + new) / (usageCount + 1)
      BigDecimal total =
          this.avgSuccessRate.multiply(BigDecimal.valueOf(this.usageCount)).add(newRate);
      this.avgSuccessRate =
          total.divide(BigDecimal.valueOf(this.usageCount + 1L), 2, RoundingMode.HALF_UP);
    }
  }
}
