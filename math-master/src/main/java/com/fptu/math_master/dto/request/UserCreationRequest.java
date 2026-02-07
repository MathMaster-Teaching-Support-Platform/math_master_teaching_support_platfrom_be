package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  String userName;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  String password;

  @NotBlank(message = "Full name is required")
  @Size(max = 255, message = "Full name must not exceed 255 characters")
  String fullName;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 255, message = "Email must not exceed 255 characters")
  String email;

  @Size(max = 20, message = "Phone number must not exceed 20 characters")
  String phoneNumber;

  Gender gender;

  String avatar;

  LocalDate dob;

  @Size(max = 100, message = "Code must not exceed 100 characters")
  String code;

  Set<String> roles;
}

