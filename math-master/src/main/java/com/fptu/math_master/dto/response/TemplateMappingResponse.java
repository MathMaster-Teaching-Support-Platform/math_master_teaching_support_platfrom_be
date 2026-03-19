package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import java.math.BigDecimal;
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
public class TemplateMappingResponse {

  private UUID id;
  private UUID examMatrixId;
  private UUID templateId;
  private String templateName;
  private CognitiveLevel cognitiveLevel;
  private Integer questionCount;
  private BigDecimal pointsPerQuestion;
  private BigDecimal totalPoints;
  private Instant createdAt;
  private Instant updatedAt;
}
