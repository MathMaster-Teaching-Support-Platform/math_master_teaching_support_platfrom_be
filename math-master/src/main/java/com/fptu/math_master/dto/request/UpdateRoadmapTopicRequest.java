package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
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
public class UpdateRoadmapTopicRequest {

  private String title;

  private String description;

  private Integer sequenceOrder;

  private Integer priority;

  private Integer estimatedHours;

  private List<UUID> lessonIds;

  private UUID topicAssessmentId;

  private BigDecimal passThresholdPercentage;

  private QuestionDifficulty difficulty;

  private TopicStatus status;
}
