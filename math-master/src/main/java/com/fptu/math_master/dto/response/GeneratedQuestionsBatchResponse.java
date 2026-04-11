package com.fptu.math_master.dto.response;

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
public class GeneratedQuestionsBatchResponse {

  private Integer totalRequested;
  private Integer totalGenerated;
  private List<UUID> generatedQuestionIds;
  private List<String> warnings;
}
