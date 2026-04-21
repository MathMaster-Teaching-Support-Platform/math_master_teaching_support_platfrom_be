package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomCourseSectionRequest {

  @NotBlank(message = "title is required")
  @Size(max = 255, message = "title must not exceed 255 characters")
  private String title;

  private String description;

  @NotNull(message = "orderIndex is required")
  private Integer orderIndex;
}
