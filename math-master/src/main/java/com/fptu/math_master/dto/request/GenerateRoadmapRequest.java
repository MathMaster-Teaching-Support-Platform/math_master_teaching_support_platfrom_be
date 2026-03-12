package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.RoadmapGenerationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to generate a new learning roadmap for a student
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateRoadmapRequest {

  @NotBlank(message = "Subject is required")
  @Schema(description = "Subject name (e.g., 'Algebra', 'Geometry')", example = "Algebra")
  private String subject;

  @NotBlank(message = "Grade level is required")
  @Schema(description = "Grade level (e.g., 'Grade 9', 'Grade 10')", example = "Grade 9")
  private String gradeLevel;

  @NotNull(message = "Generation type is required")
  @Schema(
      description =
          "How to generate roadmap: PERSONALIZED (based on performance), DEFAULT (grade-based), TEACHER_ASSIGNED (by teacher)",
      example = "PERSONALIZED")
  private RoadmapGenerationType generationType;

  @Schema(
      description = "Optional description of the roadmap",
      example = "Focus on weak areas in algebra")
  private String description;

  @Builder.Default
  @Schema(
      description = "Whether to include weak topic analysis from performance data",
      example = "true")
  private Boolean analyzePerformanceData = true;

  @Builder.Default
  @Schema(
      description = "Whether to create default curriculum if performance data is insufficient",
      example = "true")
  private Boolean createDefaultIfNeeded = true;
}
