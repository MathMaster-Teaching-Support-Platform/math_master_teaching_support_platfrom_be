package com.fptu.math_master.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchoolResponse {

  UUID id;
  String name;
  String address;
  String city;
  String district;
  String phoneNumber;
  String email;
  String website;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
