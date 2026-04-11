package com.fptu.math_master.dto.response;

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
public class StudentInCourseResponse {
  private UUID studentId;
  private String studentName;
  private String email;
  private Instant enrolledAt;
  private int completedLessons;
  private int totalLessons;
}
