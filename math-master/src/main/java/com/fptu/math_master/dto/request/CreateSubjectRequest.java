package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  @NotBlank(message = "Code is required")
  @Size(max = 50, message = "Code must not exceed 50 characters")
  @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase letters, digits, or underscores")
  private String code;

  private String description;

  @Min(value = 1, message = "gradeMin must be between 1 and 12")
  @Max(value = 12, message = "gradeMin must be between 1 and 12")
  private Integer gradeMin;

  @Min(value = 1, message = "gradeMax must be between 1 and 12")
  @Max(value = 12, message = "gradeMax must be between 1 and 12")
  private Integer gradeMax;
}
