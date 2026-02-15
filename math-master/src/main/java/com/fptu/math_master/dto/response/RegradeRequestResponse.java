package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.RegradeRequestStatus;
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
public class RegradeRequestResponse {

  private UUID id;
  private UUID submissionId;
  private UUID questionId;
  private String questionText;
  private UUID studentId;
  private String studentName;
  private String reason;
  private RegradeRequestStatus status;
  private String teacherResponse;
  private UUID reviewedBy;
  private String reviewerName;
  private Instant reviewedAt;
  private Instant createdAt;
}
