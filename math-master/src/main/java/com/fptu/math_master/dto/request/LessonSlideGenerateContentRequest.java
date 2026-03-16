package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class LessonSlideGenerateContentRequest {

  @NotNull
  @Min(1)
  @Max(12)
  private Integer gradeLevel;

  @NotNull private UUID subjectId;

  @NotNull private UUID chapterId;

  @NotNull private UUID lessonId;

  @Min(5)
  @Max(15)
  @Builder.Default
  private Integer slideCount = 10;

  @NotBlank private String additionalPrompt;
}
