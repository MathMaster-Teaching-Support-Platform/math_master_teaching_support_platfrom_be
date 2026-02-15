package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.ProfileStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileResponse {

  UUID id;
  UUID userId;
  String userName;
  String fullName;
  UUID schoolId;
  String schoolName;
  String position;
  String certificateUrl;
  String identificationDocumentUrl;
  String description;
  ProfileStatus status;
  String adminComment;
  UUID reviewedBy;
  String reviewedByName;
  LocalDateTime reviewedAt;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
