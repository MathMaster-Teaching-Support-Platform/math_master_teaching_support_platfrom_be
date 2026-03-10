package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CurriculumCategory;
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
public class CurriculumResponse {
  private UUID id;
  private String name;
  private Integer grade;
  private CurriculumCategory category;
  private String description;
  private Instant createdAt;
  private Instant updatedAt;
}
