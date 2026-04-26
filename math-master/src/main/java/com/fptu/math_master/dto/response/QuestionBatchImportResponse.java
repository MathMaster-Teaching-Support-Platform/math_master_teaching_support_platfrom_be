package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBatchImportResponse {

  private Integer totalRows;
  private Integer successCount;
  private Integer failedCount;
  private List<String> errors;
}
