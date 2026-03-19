package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the exam-matrix table — corresponds to a single <em>dạng bài</em>
 * (question type) associated with a chapter / lesson.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixRowRequest {

  /** Chapter this row belongs to (required). */
  @NotNull(message = "chapterId is required")
  private UUID chapterId;

  /** Optional: narrow to a specific lesson within the chapter. */
  private UUID lessonId;

  /**
   * Optional FK to an existing {@link com.fptu.math_master.entity.QuestionTemplate}.
   * If provided, {@code questionTypeName} defaults to the template's name.
   */
  private UUID templateId;

  /**
   * Human-readable label for the dạng bài column.
   * Required when {@code templateId} is null.
   */
  @Size(max = 500, message = "questionTypeName must not exceed 500 characters")
  private String questionTypeName;

  /**
   * Reference question numbers from the illustrative exam paper, e.g. "3,30".
   * Maps to the "Trích dẫn đề Minh Họa" column in the matrix.
   */
  @Size(max = 255, message = "referenceQuestions must not exceed 255 characters")
  private String referenceQuestions;

  /** 1-based display order within the chapter group. */
  private Integer orderIndex;

  /** The cognitive-level cells for this row (at least one). */
  @NotEmpty(message = "cells must not be empty")
  @Valid
  private List<MatrixCellRequest> cells;
}
