package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for learning materials linked to a topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicMaterialResponse {

  @Schema(description = "Material ID")
  private UUID id;

  @Schema(description = "Title of the material")
  private String resourceTitle;

  @Schema(description = "Type of material (LESSON, QUESTION, EXAMPLE, PRACTICE, ASSESSMENT)")
  private String resourceType;

  @Schema(description = "Order in learning sequence")
  private Integer sequenceOrder;

  @Schema(description = "Whether this material is required")
  private Boolean isRequired;

  @Schema(description = "Lesson ID (if applicable)")
  private UUID lessonId;

  @Schema(description = "Question ID (if applicable)")
  private UUID questionId;

  @Schema(description = "Assessment ID (if applicable)")
  private UUID assessmentId;

  @Schema(description = "Mindmap ID (if applicable)")
  private UUID mindmapId;

  @Schema(description = "Chapter ID (for context)")
  private UUID chapterId;
}
