package com.fptu.math_master.entity;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "canonical_questions",
    indexes = {
      @Index(name = "idx_canonical_questions_created_by", columnList = "created_by"),
      @Index(name = "idx_canonical_questions_type", columnList = "problem_type"),
      @Index(name = "idx_canonical_questions_difficulty", columnList = "difficulty")
    })
public class CanonicalQuestion extends BaseEntity {

  @Nationalized
  @Column(name = "title", length = 255)
  private String title;

  @Nationalized
  @Column(name = "problem_text", nullable = false, columnDefinition = "TEXT")
  private String problemText;

  @Nationalized
  @Column(name = "solution_steps", columnDefinition = "TEXT")
  private String solutionSteps;

  @Type(JsonBinaryType.class)
  @Column(name = "diagram_definition", columnDefinition = "jsonb")
  private Map<String, Object> diagramDefinition;

  @Column(name = "problem_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType problemType;

  @Column(name = "difficulty")
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @OneToMany(mappedBy = "canonicalQuestion")
  private Set<QuestionTemplate> derivedTemplates;
}