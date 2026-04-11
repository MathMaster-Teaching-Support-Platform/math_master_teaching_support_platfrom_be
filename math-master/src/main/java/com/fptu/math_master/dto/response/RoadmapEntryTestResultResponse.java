package com.fptu.math_master.dto.response;

import java.time.Instant;
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
public class RoadmapEntryTestResultResponse {

  private UUID roadmapId;
  private UUID submissionId;
  private UUID suggestedTopicId;
  private Integer score;
  private Integer studentBestScore;
  private List<RoadmapUnlockedTopicResponse> unlockedTopics;
  private List<RoadmapUnlockedTopicResponse> newlyUnlockedTopics;
  private Double scoreOnTen;
  private Integer evaluatedQuestions;
  private Integer thresholdPercentage;
  private Instant evaluatedAt;
}
