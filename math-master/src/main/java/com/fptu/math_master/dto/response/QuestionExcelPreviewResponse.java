package com.fptu.math_master.dto.response;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExcelPreviewResponse {

  private Integer totalRows;
  private Integer validRows;
  private Integer invalidRows;
  private List<PreviewRow> rows;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PreviewRow {
    private Integer rowNumber;
    private Boolean isValid;
    private CreateQuestionRequest data;
    private List<String> validationErrors;
  }
}
