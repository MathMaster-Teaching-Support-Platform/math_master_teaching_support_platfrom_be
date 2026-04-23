package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapProgressInfo {

  @Schema(description = "Zero-based current topic index")
  @JsonProperty("current_topic_index")
  private Integer currentTopicIndex;
}
