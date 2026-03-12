package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "exam_matrix_template_mappings",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_exam_matrix_template",
          columnNames = {"exam_matrix_id", "template_id"})
    },
    indexes = {
      @Index(name = "idx_exam_matrix_template_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_exam_matrix_template_template", columnList = "template_id"),
      @Index(name = "idx_exam_matrix_template_cognitive", columnList = "cognitive_level")
    })
public class ExamMatrixTemplateMapping {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "exam_matrix_id", nullable = false)
  private UUID examMatrixId;

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Column(name = "question_count", nullable = false)
  private Integer questionCount;

  @Column(name = "points_per_question", nullable = false, precision = 5, scale = 2)
  private BigDecimal pointsPerQuestion;

  @Column(name = "total_points", precision = 6, scale = 2, insertable = false, updatable = false)
  private BigDecimal totalPoints;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", insertable = false, updatable = false)
  private QuestionTemplate questionTemplate;

  @OneToMany(
      mappedBy = "examMatrixTemplateMapping",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;

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
