package com.fptu.math_master.dto.request;

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
public class LessonSlideGeneratePptxRequest {

  @NotNull private UUID lessonId;

  @NotNull private UUID templateId;

  private String additionalPrompt;
}
