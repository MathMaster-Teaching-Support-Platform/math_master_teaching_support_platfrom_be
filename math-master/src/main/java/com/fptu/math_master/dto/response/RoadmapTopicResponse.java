package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "Expected mark checkpoint on a 10-point scale")
  private Double mark;

  @Schema(description = "Required point to unlock this topic (10-point scale)")
  private Double requiredPoint;

  @Schema(description = "Whether topic is unlocked for current student")
  private Boolean unlocked;

  @Schema(description = "Assessment linked to this topic")
  private UUID topicAssessmentId;

  @Schema(description = "Linked course IDs for this topic")
  private List<UUID> courseIds;

  @Schema(description = "Linked courses for this topic")
  private List<RoadmapTopicCourseResponse> courses;

  @Schema(description = "When topic was started")
  private Instant startedAt;

  @Schema(description = "Question templates for this topic (from linked lesson)")
  private List<QuestionTemplateResponse> questionTemplates;

  @Schema(description = "Mindmaps for this topic (from linked lesson)")
  private List<MindmapResponse> mindmaps;

  @Schema(description = "Ordered learning materials attached to this topic")
  private List<TopicMaterialResponse> materials;
}
