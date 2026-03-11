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
public class QuestionBankResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private String name;
  private String description;
  private Boolean isPublic;
  private Long questionCount;
  private Instant createdAt;
  private Instant updatedAt;
}
