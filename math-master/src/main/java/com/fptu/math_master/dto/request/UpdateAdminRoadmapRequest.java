package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.RoadmapStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminRoadmapRequest {

  @Size(min = 1, max = 255, message = "Tên lộ trình phải từ 1-255 ký tự")
  private String name;

  private UUID subjectId;

  private String description;

  private Integer estimatedCompletionDays;

  private RoadmapStatus status;
}
