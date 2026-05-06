package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Happy-case exam-matrix builder.
 *
 * <p>Caller picks one bank + a set of chapters, and gives a count for each
 * cognitive level (NB/TH/VD/VDC) per chapter. The service expands this into
 * the existing matrix structure (one row per chapter, four cells per row),
 * so downstream generation, validation, PDF export, etc. keep working.
 *
 * <p>This is a <em>convenience</em> entrypoint; the full
 * {@link BuildExamMatrixRequest} flow remains available for matrices that
 * need multiple "dạng bài" rows per chapter or per-cell point overrides.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildSimpleExamMatrixRequest {

  @NotBlank(message = "name is required")
  @Size(max = 255, message = "name must not exceed 255 characters")
  private String name;

  private String description;

  /**
   * Optional default bank stored on the matrix as a hint for "tạo đề thi từ ma trận".
   * <p>
   * The matrix is a pure blueprint: chapter-cell counts only define the
   * structure. The bank used for question selection is picked at
   * assessment-generation time. Pre-filling here only seeds the FE picker.
   */
  private UUID questionBankId;

  /**
   * Target school-grade level (lớp), e.g. 9, 10, 11, 12.
   * Required so the matrix is anchored to a single grade.
   */
  @NotNull(message = "gradeLevel is required")
  @Min(value = 1, message = "gradeLevel must be between 1 and 12")
  @Max(value = 12, message = "gradeLevel must be between 1 and 12")
  private Integer gradeLevel;

  /**
   * Default points per question — applied to every cell unless that cell
   * provides its own override. Optional; the matrix-level point cap is
   * computed from the cell counts.
   */
  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
  private BigDecimal pointsPerQuestion;

  /** Whether this matrix can be cloned for other assessments. */
  private Boolean isReusable;

  @NotEmpty(message = "chapters must not be empty")
  @Valid
  private List<ChapterCognitiveCounts> chapters;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChapterCognitiveCounts {

    @NotNull(message = "chapterId is required")
    private UUID chapterId;

    @Min(value = 0, message = "nb must be >= 0")
    private Integer nb;

    @Min(value = 0, message = "th must be >= 0")
    private Integer th;

    @Min(value = 0, message = "vd must be >= 0")
    private Integer vd;

    @Min(value = 0, message = "vdc must be >= 0")
    private Integer vdc;

    /** Optional per-chapter override, beats the matrix-level pointsPerQuestion. */
    @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
    private BigDecimal pointsPerQuestion;
  }
}
