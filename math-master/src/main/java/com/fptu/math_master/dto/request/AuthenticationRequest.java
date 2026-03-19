package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 50, message = "Email must not exceed 50 characters")
  String email;

  @NotBlank(message = "Password is required")
  String password;
}
