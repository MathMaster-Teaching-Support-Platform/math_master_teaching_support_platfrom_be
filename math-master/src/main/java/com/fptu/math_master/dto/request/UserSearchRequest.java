package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSearchRequest {

  String keyword; // Search in username, fullName, email

  String userName;

  String email;

  String fullName;

  Gender gender;

  Status status;

  String code;

  LocalDate dobFrom;

  LocalDate dobTo;

  String roleName;
}

