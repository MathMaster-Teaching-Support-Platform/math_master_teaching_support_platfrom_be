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
public class UpdateCourseRequest {

  @Size(max = 255, message = "title must not exceed 255 characters")
  private String title;

  private String description;
}
