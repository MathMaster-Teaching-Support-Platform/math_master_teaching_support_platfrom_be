package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSlideGeneratedFileMetadataUpdateRequest {

  @Size(max = 255, message = "Slide name must be at most 255 characters")
  private String name;

  @Size(max = 2048, message = "Thumbnail URL must be at most 2048 characters")
  private String thumbnail;
}
