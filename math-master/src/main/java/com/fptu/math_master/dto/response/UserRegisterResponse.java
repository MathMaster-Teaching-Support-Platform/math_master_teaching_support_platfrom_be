package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRegisterResponse {

  UUID id;

  String userName;

  String email;

  Status status;

  Instant createdDate;

  String createdBy;

  Instant updatedDate;

  String updatedBy;
}
