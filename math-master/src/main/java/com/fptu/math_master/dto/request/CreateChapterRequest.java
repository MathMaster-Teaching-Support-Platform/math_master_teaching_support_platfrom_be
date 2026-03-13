package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreateChapterRequest {

  @NotNull(message = "curriculumId is required")
  private UUID curriculumId;

  @NotBlank(message = "title is required")
  @Size(max = 255, message = "title must not exceed 255 characters")
  private String title;

  private String description;

  /** Display order within the curriculum. Auto-assigned (last+1) if omitted. */
  @Positive(message = "orderIndex must be positive")
  private Integer orderIndex;
}
