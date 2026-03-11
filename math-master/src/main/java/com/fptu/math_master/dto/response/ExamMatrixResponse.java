package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.MatrixStatus;
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
public class ExamMatrixResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private String name;
  private String description;
  private Boolean isReusable;
  private MatrixStatus status;
  private Integer templateMappingCount;
  private List<TemplateMappingResponse> templateMappings;
  private Instant createdAt;
  private Instant updatedAt;
}
