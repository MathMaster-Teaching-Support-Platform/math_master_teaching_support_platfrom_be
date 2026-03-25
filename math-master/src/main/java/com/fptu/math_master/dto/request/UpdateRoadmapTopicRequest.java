package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
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

  private Double mark;

  private List<UUID> lessonIds;

  private List<UUID> slideLessonIds;

  private List<UUID> assessmentIds;

  private List<UUID> lessonPlanIds;

  private List<UUID> mindmapIds;

  private UUID topicAssessmentId;

  private QuestionDifficulty difficulty;

  private TopicStatus status;
}
