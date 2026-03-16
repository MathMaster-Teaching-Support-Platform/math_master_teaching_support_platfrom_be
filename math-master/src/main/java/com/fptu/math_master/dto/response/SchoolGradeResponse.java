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
public class SchoolGradeResponse {

  private UUID id;
  private Integer gradeLevel;
  private String name;
  private String description;
  private Boolean active;
  private Instant createdAt;
  private Instant updatedAt;
}
