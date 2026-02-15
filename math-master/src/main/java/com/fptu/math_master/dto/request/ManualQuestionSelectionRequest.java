package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ManualQuestionSelectionRequest {

  @NotNull(message = "Matrix cell ID is required")
  private UUID matrixCellId;

  @NotEmpty(message = "At least one question must be selected")
  private List<UUID> questionIds;
}
