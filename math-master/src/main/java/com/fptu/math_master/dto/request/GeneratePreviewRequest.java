package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /exam-matrices/{matrixId}/cells/{cellId}/generate-preview.
 * All generation is in-memory; nothing is written to the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePreviewRequest {

  /** ID of the question template to use for generation. */
  @NotNull(message = "templateId is required")
  private UUID templateId;

  /**
   * How many candidate questions to generate.
   * Must be between 1 and 50.
   */
  @NotNull(message = "count is required")
  @Min(value = 1, message = "count must be at least 1")
  @Max(value = 50, message = "count must not exceed 50")
  private Integer count;

  /**
   * Optional difficulty override.
   * When null the difficulty is determined from the template's difficultyRules for each candidate.
   * When provided must be EASY / MEDIUM / HARD.
   */
  private QuestionDifficulty difficulty;

  /**
   * Optional seed for reproducible previews.
   * When null a random seed is used.
   */
  private Long seed;
}

