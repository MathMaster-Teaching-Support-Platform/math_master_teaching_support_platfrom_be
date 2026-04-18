package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CourseProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

  /**
   * Determines the course flow.
   * MINISTRY → existing flow (subjectId + schoolGradeId required).
   * CUSTOM   → Udemy-style (subjectId + schoolGradeId optional).
   */
  @NotNull(message = "provider is required")
  private CourseProvider provider;

  /** Required when provider = MINISTRY. */
  private UUID subjectId;

  /** Required when provider = MINISTRY. */
  private UUID schoolGradeId;

  @NotBlank(message = "title is required")
  @Size(max = 255, message = "title must not exceed 255 characters")
  private String title;

  private String description;
}
