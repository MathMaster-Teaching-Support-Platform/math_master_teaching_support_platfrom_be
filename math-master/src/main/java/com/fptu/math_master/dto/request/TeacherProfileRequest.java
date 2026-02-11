package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileRequest {

  @NotNull(message = "School ID is required")
  UUID schoolId;

  @NotBlank(message = "Position is required")
  @Size(max = 100, message = "Position must not exceed 100 characters")
  String position;

  String certificateUrl;

  String identificationDocumentUrl;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  String description;
}
