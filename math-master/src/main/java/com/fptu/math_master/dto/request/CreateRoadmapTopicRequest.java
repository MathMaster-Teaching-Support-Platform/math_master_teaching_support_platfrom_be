package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoadmapTopicRequest {

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotNull(message = "Sequence order is required")
  private Integer sequenceOrder;

  private Integer priority;

  private Integer estimatedHours;

  private List<UUID> lessonIds;

  private List<UUID> slideLessonIds;

  private List<UUID> assessmentIds;

  private List<UUID> lessonPlanIds;

  private List<UUID> mindmapIds;

  private UUID topicAssessmentId;

  private BigDecimal passThresholdPercentage;

  @NotNull(message = "Difficulty is required")
  private QuestionDifficulty difficulty;
}
