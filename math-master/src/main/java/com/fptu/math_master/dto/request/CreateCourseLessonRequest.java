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
public class CreateCourseLessonRequest {

  @NotNull(message = "lessonId is required")
  private UUID lessonId;

  private String videoTitle;

  private Integer orderIndex;

  @Builder.Default
  private boolean isFreePreview = false;

  /** JSON string of List<MaterialItem>: [{type, title, url}] */
  private String materials;
}
