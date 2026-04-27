package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

  @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
  @Pattern(
      regexp =
          "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
      message =
          "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
  String password;

  @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
  String fullName;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 50, message = "Email must not exceed 50 characters")
  String email;

  @Pattern(
      regexp = "^(\\+84|0)(3[2-9]|5[25689]|7[06-9]|8[0-689]|9[0-9])[0-9]{7}$",
      message =
          "Phone number must be a valid Vietnamese phone number (e.g. 0912345678 or +84912345678)")
  String phoneNumber;

  Gender gender;

  @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
  String avatar;

  @Past(message = "Date of birth must be in the past")
  LocalDate dob;

  @Size(max = 100, message = "Code must not exceed 100 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_-]*$",
      message = "Code must contain only alphanumeric characters, underscores, and hyphens")
  String code;

  @Size(max = 12, message = "School grades must not exceed 12 items (grades 1-12)")
  Set<UUID> schoolGradeIds;

  Status status;

  @Size(max = 10, message = "Roles must not exceed 10 items")
  Set<String> roles;
}
