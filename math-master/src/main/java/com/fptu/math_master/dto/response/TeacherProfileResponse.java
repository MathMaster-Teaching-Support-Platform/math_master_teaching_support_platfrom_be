package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.ProfileStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileResponse {

  Long id;
  Integer userId;
  String userName;
  String fullName;
  Long schoolId;
  String schoolName;
  String position;
  String certificateUrl;
  String identificationDocumentUrl;
  String description;
  ProfileStatus status;
  String adminComment;
  Integer reviewedBy;
  String reviewedByName;
  LocalDateTime reviewedAt;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
