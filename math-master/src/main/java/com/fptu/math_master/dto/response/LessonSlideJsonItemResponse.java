package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSlideJsonItemResponse {

  private Integer slideNumber;
  private String slideType;
  private String heading;
  private String content;

  /**
   * When outputFormat=LATEX, contains the QuickLaTeX-rendered image URL for this slide.
   * FE should display &lt;img src={previewImageUrl}/&gt; instead of raw content text.
   * Null for PLAIN_TEXT and HYBRID modes.
   */
  private String previewImageUrl;
}
