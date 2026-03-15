package com.fptu.math_master.dto.response;

import java.time.Instant;
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
public class SchoolResponse {

  UUID id;
  String name;
  String address;
  String city;
  String district;
  String phoneNumber;
  String email;
  String website;
  Instant createdAt;
  Instant updatedAt;
}
