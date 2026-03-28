package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
  private UUID id;
  private UUID courseId;
  private String courseTitle;
  private UUID studentId;
  private String studentName;
  private EnrollmentStatus status;
  private Instant enrolledAt;
  private Instant createdAt;
  private Instant updatedAt;
}
