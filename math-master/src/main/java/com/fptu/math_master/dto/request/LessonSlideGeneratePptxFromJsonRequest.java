package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.LessonSlideOutputFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSlideGeneratePptxFromJsonRequest {

  @NotNull private UUID lessonId;

  @NotNull private UUID templateId;

  @Valid @NotEmpty private List<LessonSlideJsonItemRequest> slides;

  /** Output format used during content generation. When LATEX, each slide is rendered as a single
   *  full-content image via QuickLaTeX (supports TikZ geometry). */
  private LessonSlideOutputFormat outputFormat;
}
