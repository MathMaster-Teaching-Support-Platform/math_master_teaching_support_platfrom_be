package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateLessonContentRequest {

  /** Vietnamese math grade level, e.g. "Lớp 10", "Lớp 11", "Lớp 12" */
  @NotBlank(message = "gradeLevel is required")
  private String gradeLevel;

  /**
   * Math subject/topic area, e.g. "Đại số", "Hình học", "Giải tích", "Tổ hợp – Xác suất"
   */
  @NotBlank(message = "subject is required")
  private String subject;

  /**
   * Optional extra hint for Gemini, e.g. "focus on limit and derivative for grade 11"
   */
  private String additionalContext;

  /**
   * Number of lessons to generate. Default 5, max 20.
   */
  @Min(1)
  @Max(20)
  @Builder.Default
  private int lessonCount = 5;

  /**
   * Number of chapters per lesson. Default 4, max 10.
   */
  @Min(1)
  @Max(10)
  @Builder.Default
  private int chaptersPerLesson = 4;

  /**
   * Whether to skip lessons that already exist for this gradeLevel + subject combo.
   * Default true (idempotent).
   */
  @Builder.Default
  private boolean skipIfExists = true;
}

