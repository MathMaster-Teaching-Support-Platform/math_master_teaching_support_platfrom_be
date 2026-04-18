package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
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

  private QuestionDifficulty difficulty;

  /** Update the linked course for this topic. */
  private UUID courseId;

  /** Optional non-blocking status update for progress tracking. */
  private TopicStatus status;
}
