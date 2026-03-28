package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseLessonRequest {

  private String videoTitle;

  private Integer orderIndex;

  private Boolean isFreePreview;

  /** JSON string of List<MaterialItem>: [{type, title, url}] */
  private String materials;
}
