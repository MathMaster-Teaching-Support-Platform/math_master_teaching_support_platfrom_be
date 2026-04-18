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
 * Response for a topic within a roadmap.
 *
 * <p>Topics are always accessible — no locking. The linked {@code courses} give students a
 * preview and direct enroll/open CTAs.
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

  @Schema(description = "Topic title")
  private String title;

  @Schema(description = "Topic description")
  private String description;

  @Schema(description = "Current status (non-blocking progress tracking)")
  private TopicStatus status;

  @Schema(description = "Difficulty level")
  private QuestionDifficulty difficulty;

  @Schema(description = "Visual order in the roadmap (guidance only, not a lock gate)")
  private Integer sequenceOrder;

  @Schema(description = "The courses linked to this topic")
  private List<RoadmapTopicCourseResponse> courses;

  @Schema(description = "When this topic was started by the student")
  private Instant startedAt;
}
