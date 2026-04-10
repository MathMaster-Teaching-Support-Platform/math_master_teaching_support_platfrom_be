package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
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
public class BulkAssignQuestionsToBankRequest {

  @NotEmpty(message = "questionIds must not be empty")
  private List<UUID> questionIds;
}
