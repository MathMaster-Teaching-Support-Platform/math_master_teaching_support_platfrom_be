package com.fptu.math_master.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to create or update student learning wishes and preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentWishRequest {

  @NotBlank(message = "Subject is required")
  @Schema(description = "Subject name (e.g., 'Algebra', 'Geometry')", example = "Algebra")
  private String subject;

  @Schema(description = "Grade level (e.g., 'Grade 9', 'Grade 10')", example = "Grade 9")
  private String gradeLevel;

  @NotBlank(message = "Learning goals are required")
  @Schema(
      description = "Description of learning goals",
      example =
          "I want to improve my skills in solving quadratic equations and master factorization techniques")
  private String learningGoals;

  @Schema(
      description = "Preferred topics (comma-separated or JSON list)",
      example = "Quadratic Equations, Factorization, Polynomials")
  private String preferredTopics;

  @Schema(
      description = "Weak areas to improve (comma-separated or JSON list)",
      example = "Solving Systems of Equations, Word Problems")
  private String weakAreasToImprove;

  @NotNull(message = "Daily study minutes is required")
  @Min(value = 15, message = "Daily study time must be at least 15 minutes")
  @Schema(description = "Minutes per day student can study", example = "60")
  private Integer dailyStudyMinutes;

  @Min(value = 0)
  @Schema(description = "Target accuracy percentage (0-100)", example = "90")
  private Integer targetAccuracyPercentage;

  @Schema(
      description = "Learning style preference (visual, practice, theory-first, mixed)",
      example = "practice")
  private String learningStylePreference;

  @Schema(description = "Whether student prefers difficult challenges", example = "false")
  private Boolean preferDifficultChallenges;
}
