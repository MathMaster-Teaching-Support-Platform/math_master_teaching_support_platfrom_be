package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    name = "question_templates",
    indexes = {
      @Index(name = "idx_question_templates_created_by", columnList = "created_by"),
      @Index(name = "idx_question_templates_tags", columnList = "tags"),
      @Index(name = "idx_question_templates_cognitive", columnList = "cognitive_level"),
      @Index(name = "idx_question_templates_public", columnList = "is_public")
    })
public class QuestionTemplate {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Nationalized
  @Column(name = "name", nullable = false, columnDefinition = "TEXT")
  private String name;

  @Nationalized
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "template_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType templateType;

  /**
   * Multi-language support for question template text Example: {"en": "What is {x} + {y}?", "vi":
   * "{x} + {y} bằng bao nhiêu?"}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "template_text", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> templateText;

  /**
   * Parameters that can be substituted in the template Example: {"x": {"type": "integer", "min": 1,
   * "max": 100}, "y": {"type": "integer", "min": 1, "max": 100}}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "parameters", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> parameters;

  /** Formula to calculate the correct answer Example: "x + y" or "Math.sqrt(x^2 + y^2)" */
  @Column(name = "answer_formula", nullable = false, columnDefinition = "TEXT")
  private String answerFormula;

  /**
   * Configuration for generating multiple choice options Example: {"type": "around_answer",
   * "count": 4, "range": 10}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "options_generator", columnDefinition = "jsonb")
  private Map<String, Object> optionsGenerator;

  /**
   * Rules for determining difficulty based on parameters Example: {"easy": "x < 10 AND y < 10",
   * "medium": "x < 50 AND y < 50", "hard": "x >= 50 OR y >= 50"}
   */
  @Type(JsonBinaryType.class)
  @Column(name = "difficulty_rules", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> difficultyRules;

  /** Constraints for parameter generation Example: ["x < y", "x + y < 1000"] */
  @Type(StringArrayType.class)
  @Column(name = "constraints", columnDefinition = "TEXT[]")
  private String[] constraints;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Type(StringArrayType.class)
  @Column(name = "tags", nullable = false, columnDefinition = "TEXT[]")
  private String[] tags;

  @Column(name = "is_public", nullable = false)
  private Boolean isPublic = false;

  @Column(name = "usage_count", nullable = false)
  private Integer usageCount = 0;

  @Column(name = "avg_success_rate", precision = 5, scale = 2)
  private BigDecimal avgSuccessRate;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TemplateStatus status = TemplateStatus.DRAFT;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @OneToMany(mappedBy = "questionTemplate", cascade = CascadeType.ALL)
  private Set<Question> questions;

  @PrePersist
  public void prePersist() {
    if (isPublic == null) isPublic = false;
    if (usageCount == null) usageCount = 0;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
    if (status == null) status = TemplateStatus.DRAFT;
  }
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
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
          total.divide(BigDecimal.valueOf(this.usageCount + 1), 2, RoundingMode.HALF_UP);
    }
  }
}
