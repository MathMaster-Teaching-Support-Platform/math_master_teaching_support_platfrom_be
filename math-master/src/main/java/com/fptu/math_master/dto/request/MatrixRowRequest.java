package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
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
 * 
 * Phase 3: questionBankId removed from row level.
 * The bank is now specified at the matrix level (BuildExamMatrixRequest.questionBankId).
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
   * Selected difficulty for this row (EASY/MEDIUM/HARD).
   */
  private QuestionDifficulty questionDifficulty;

  /**
   * Human-readable label for the dạng bài column.
   * Required for bank-only row.
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

  /**
   * Optional cells for this row.
   * FE can create row first, then call the dedicated row-cells endpoint.
   */
  private List<MatrixCellRequest> cells;
}
