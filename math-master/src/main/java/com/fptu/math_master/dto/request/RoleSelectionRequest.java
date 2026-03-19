package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleSelectionRequest {
  @NotBlank(message = "Role is required")
  String role;

  // Optional for students
  String userName;
  String fullName;
}
