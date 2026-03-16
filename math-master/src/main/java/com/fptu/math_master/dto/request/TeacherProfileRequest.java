package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileRequest {

  @NotBlank(message = "School name is required")
  String schoolName;

  String schoolAddress;

  String schoolWebsite;

  @NotBlank(message = "Position is required")
  @Size(max = 100, message = "Position must not exceed 100 characters")
  String position;

  @NotBlank(message = "Document type is required")
  String documentType;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  String description;
}
