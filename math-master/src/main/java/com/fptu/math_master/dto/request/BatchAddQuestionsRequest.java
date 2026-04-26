package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchAddQuestionsRequest {

  @NotEmpty(message = "Danh sách câu hỏi không được để trống")
  private List<UUID> questionIds;
}
