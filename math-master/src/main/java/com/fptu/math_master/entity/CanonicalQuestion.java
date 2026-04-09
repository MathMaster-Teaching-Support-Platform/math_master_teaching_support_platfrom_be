package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
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
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

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
      @Index(name = "idx_canonical_questions_cognitive", columnList = "cognitive_level")
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

  @Column(name = "diagram_definition", columnDefinition = "TEXT")
  private String diagramDefinition;

  @Column(name = "problem_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionType problemType;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private User creator;

  @OneToMany(mappedBy = "canonicalQuestion")
  private Set<QuestionTemplate> derivedTemplates;
}