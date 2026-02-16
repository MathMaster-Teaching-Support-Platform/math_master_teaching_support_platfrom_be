package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "matrix_cells",
    indexes = {
      @Index(name = "idx_matrix_cells_matrix", columnList = "matrix_id"),
      @Index(name = "idx_matrix_cells_chapter", columnList = "chapter_id"),
      @Index(name = "idx_matrix_cells_cognitive", columnList = "cognitive_level"),
      @Index(name = "idx_matrix_cells_difficulty", columnList = "difficulty")
    })
public class MatrixCell {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "matrix_id", nullable = false)
  private UUID matrixId;

  @Column(name = "chapter_id")
  private UUID chapterId;

  @Size(max = 255)
  @Column(name = "topic", length = 255)
  private String topic;

  @Column(name = "cognitive_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private CognitiveLevel cognitiveLevel;

  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private QuestionDifficulty difficulty;

  @Column(name = "question_type")
  @Enumerated(EnumType.STRING)
  private QuestionType questionType;

  @Column(name = "num_questions", nullable = false)
  private Integer numQuestions;

  @Column(name = "points_per_question", nullable = false, precision = 5, scale = 2)
  private BigDecimal pointsPerQuestion;

  @Column(name = "total_points", precision = 6, scale = 2, insertable = false, updatable = false)
  private BigDecimal totalPoints;

  @Lob
  @Nationalized
  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @OneToMany(mappedBy = "matrixCell", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MatrixQuestionMapping> matrixQuestionMappings;

  @PrePersist
  public void prePersist() {
    if (numQuestions == null) numQuestions = 0;
    if (pointsPerQuestion == null) pointsPerQuestion = BigDecimal.valueOf(1.0);
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
