package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured request for building an exam matrix (ma trận đề thi) in one call.
 * <p>
 * Mirrors the matrix layout shown to teachers:
 * <pre>
 * Lớp | Môn | Chương | Dạng bài | Ref | NB | TH | VD | VDC | Tổng
 * </pre>
 * The caller specifies one {@link MatrixRowRequest} per "dạng bài" row and
 * each row contains one {@link MatrixCellRequest} per cognitive level used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildExamMatrixRequest {

  @NotBlank(message = "name is required")
  @Size(max = 255, message = "name must not exceed 255 characters")
  private String name;

  private String description;

  /**
   * Optional default bank stored on the matrix as a hint for "tạo đề thi từ ma trận".
   * <p>
   * The matrix is now a pure blueprint: the bank used for question selection is
   * picked at assessment-generation time via
   * {@link GenerateAssessmentQuestionsRequest#questionBankId}. Setting this here
   * only pre-fills the picker on the FE; it never restricts which bank can be
   * used to generate an assessment from this matrix.
   */
  private UUID questionBankId;

  /**
   * Number of parts in the exam (1-3).
   * DEPRECATED: Use parts[] instead for configurable part types.
   * If parts[] is provided, numberOfParts is derived from parts.length.
   * Default: 1 (MCQ only)
   */
  @Min(value = 1, message = "numberOfParts must be 1, 2, or 3")
  @Max(value = 3, message = "numberOfParts must be 1, 2, or 3")
  @Builder.Default
  private Integer numberOfParts = 1;

  /**
   * Configurable parts for the exam matrix.
   * If provided, this takes precedence over numberOfParts.
   * Each part defines its question type (MCQ, TRUE_FALSE, or SHORT_ANSWER).
   * Min: 1 part, Max: 3 parts.
   */
  @Valid
  private List<ExamMatrixPartRequest> parts;

  /**
   * Target school-grade level (lớp), e.g. 10, 11, 12.
   * Optional - can be inferred from chapters in the matrix.
   */
  @Min(value = 1, message = "gradeLevel must be between 1 and 12")
  @Max(value = 12, message = "gradeLevel must be between 1 and 12")
  private Integer gradeLevel;

  /** Whether this matrix can be cloned / reused for other assessments. */
  private Boolean isReusable;

  /** Optional cap on total questions (validation reference only). */
  @Positive(message = "totalQuestionsTarget must be > 0")
  private Integer totalQuestionsTarget;

  /** Optional cap on total points (validation reference only). */
  @DecimalMin(value = "0.01", message = "totalPointsTarget must be > 0")
  private BigDecimal totalPointsTarget;

  /** All rows (dạng bài) in the matrix, across all chapters and lessons. */
  @NotEmpty(message = "rows must not be empty")
  @Valid
  private List<MatrixRowRequest> rows;
}
