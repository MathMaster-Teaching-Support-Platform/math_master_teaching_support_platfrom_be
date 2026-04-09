package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LatexRenderRequest {

  @NotBlank(message = "latex is required")
  String latex;

  // Optional: when provided, the rendered URL is persisted back to this question.
  UUID questionId;

  @Valid LatexRenderOptions options;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class LatexRenderOptions {
    String fontSize;
    String color;
    Integer mode;
    String preamble;
  }
}
