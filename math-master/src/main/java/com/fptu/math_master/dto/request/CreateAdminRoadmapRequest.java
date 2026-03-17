package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request to create a general roadmap template for all students
 * Example: "Toán học lớp 6 cho người mới bắt đầu"
 * Admin only creates the template, students browse and choose it
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAdminRoadmapRequest {

  @NotBlank(message = "Roadmap name is required (e.g., 'Toán học lớp 6 cho người mới bắt đầu')")
  String name;

  @NotBlank(message = "Subject is required")
  String subject;

  @NotBlank(message = "Grade level is required")
  String gradeLevel;

  @NotBlank(message = "Description is required")
  String description;

  @Builder.Default
  Integer estimatedDays = 30;
}
