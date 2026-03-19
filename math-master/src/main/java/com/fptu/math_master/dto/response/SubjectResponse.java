package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {

  private UUID id;
  private String name;
  private String code;
  private String description;
  private Integer gradeMin;
  private Integer gradeMax;
  private Integer primaryGradeLevel;
  private java.util.UUID schoolGradeId;
  private Boolean isActive;

  /** Grade levels this subject is mapped to. */
  private List<Integer> gradeLevels;

  private Instant createdAt;
  private Instant updatedAt;
}
