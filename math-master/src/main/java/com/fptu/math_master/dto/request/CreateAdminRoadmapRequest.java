package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminRoadmapRequest {

  @NotNull(message = "Student ID is required")
  private UUID studentId;

  @NotBlank(message = "Subject is required")
  private String subject;

  @NotBlank(message = "Grade level is required")
  private String gradeLevel;

  private String description;

  private Integer estimatedCompletionDays;
}
