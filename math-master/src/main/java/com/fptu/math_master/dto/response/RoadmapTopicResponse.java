package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for a topic within a roadmap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapTopicResponse {

  @Schema(description = "Topic ID")
  private UUID id;

  @Schema(description = "Topic title/name")
  private String title;

  @Schema(description = "Topic description")
  private String description;

  @Schema(description = "Current status of the topic")
  private TopicStatus status;

  @Schema(description = "Difficulty level")
  private QuestionDifficulty difficulty;

  @Schema(description = "Order in learning sequence")
  private Integer sequenceOrder;

  @Schema(description = "Priority (lower = more important)")
  private Integer priority;

  @Schema(description = "Progress percentage for this topic")
  private BigDecimal progressPercentage;

  @Schema(description = "Number of completed subtopics")
  private Integer completedSubTopics;

  @Schema(description = "Total subtopics in this topic")
  private Integer totalSubTopics;

  @Schema(description = "Estimated hours to complete")
  private Integer estimatedHours;

  @Schema(description = "When topic was started")
  private Instant startedAt;

  @Schema(description = "When topic was completed")
  private Instant completedAt;

  @Schema(description = "Subtopics under this topic")
  private List<RoadmapSubtopicResponse> subtopics;

  @Schema(description = "Learning materials for this topic")
  private List<TopicMaterialResponse> materials;
}
