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
public class CourseReviewResponse {
  private UUID id;
  private UUID courseId;
  private UUID studentId;
  private String studentName;
  private String studentAvatar;
  private Integer rating;
  private String comment;
  private String instructorReply;
  private java.time.Instant repliedAt;
  private Instant createdAt;
  private Instant updatedAt;
}
