package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "matrix_question_mapping",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_matrix_question_mapping",
          columnNames = {"matrix_cell_id", "question_id"})
    },
    indexes = {
      @Index(name = "idx_matrix_question_mapping_cell", columnList = "matrix_cell_id"),
      @Index(name = "idx_matrix_question_mapping_question", columnList = "question_id"),
      @Index(name = "idx_matrix_question_mapping_selected", columnList = "is_selected")
    })
public class MatrixQuestionMapping {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "matrix_cell_id", nullable = false)
  private UUID matrixCellId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "is_selected")
  private Boolean isSelected;

  @Column(name = "selection_priority")
  private Integer selectionPriority;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "matrix_cell_id", insertable = false, updatable = false)
  private MatrixCell matrixCell;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @PrePersist
  public void prePersist() {
    if (isSelected == null) isSelected = false;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
