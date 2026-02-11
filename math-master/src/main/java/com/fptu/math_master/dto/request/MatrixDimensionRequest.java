package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixDimensionRequest {

  @NotEmpty(message = "At least one chapter must be selected")
  private List<UUID> chapterIds;

  @NotEmpty(message = "At least one cognitive level must be selected")
  private List<CognitiveLevel> cognitiveLevels;
}

