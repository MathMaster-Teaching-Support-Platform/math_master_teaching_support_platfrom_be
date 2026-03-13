package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

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
  private Boolean isActive;
  /** Grade levels this subject is mapped to. */
  private List<Integer> gradeLevels;
  private Instant createdAt;
  private Instant updatedAt;
}
