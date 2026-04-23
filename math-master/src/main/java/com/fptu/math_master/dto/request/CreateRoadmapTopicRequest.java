package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotNull(message = "Difficulty is required")
  private QuestionDifficulty difficulty;

  private Double mark;

  @NotNull(message = "courseId is required")
  private UUID courseId;
}
