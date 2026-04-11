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
public class TemplateBatchImportResponse {

  private Integer totalRows;
  private Integer successCount;
  private Integer failedCount;
  private List<QuestionTemplateResponse> successfulTemplates;
  private List<ImportErrorDetail> errors;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImportErrorDetail {
    private Integer rowNumber;
    private String rowName;
    private String field;
    private String message;
  }
}
