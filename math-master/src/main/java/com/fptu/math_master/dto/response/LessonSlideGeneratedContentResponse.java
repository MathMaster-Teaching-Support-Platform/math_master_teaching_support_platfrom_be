package com.fptu.math_master.dto.response;

import java.util.UUID;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSlideGeneratedContentResponse {

  private UUID subjectId;
  private UUID chapterId;
  private UUID lessonId;
  private String lessonTitle;
  private Integer slideCount;
  private List<LessonSlideJsonItemResponse> slides;
  private String additionalPrompt;
}
