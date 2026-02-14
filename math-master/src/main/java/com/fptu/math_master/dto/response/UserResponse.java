package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

  UUID id;

  String userName;

  String fullName;

  String email;

  String phoneNumber;

  Gender gender;

  String avatar;

  LocalDate dob;

  String code;

  Status status;

  String banReason;

  Instant banDate;

  Set<String> roles;

  Instant createdDate;

  String createdBy;

  Instant updatedDate;

  String updatedBy;
}

