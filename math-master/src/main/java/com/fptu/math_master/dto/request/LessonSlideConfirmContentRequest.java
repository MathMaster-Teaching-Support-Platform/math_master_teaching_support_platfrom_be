package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSlideConfirmContentRequest {

  @NotBlank private String lessonContent;

  private String summary;

  private String learningObjectives;
}
