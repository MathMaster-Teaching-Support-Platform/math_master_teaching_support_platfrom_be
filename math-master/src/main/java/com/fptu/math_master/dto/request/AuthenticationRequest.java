package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  String email;

  @NotBlank(message = "Password is required")
  String password;
}
