package com.fptu.math_master.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportQuestionsResponse {

  @Schema(description = "Total rows processed")
  private Integer totalRows;

  @Schema(description = "Successfully imported questions")
  private Integer successCount;

  @Schema(description = "Failed imports")
  private Integer failureCount;

  @Schema(description = "Details of each import result")
  private List<ImportRowResult> results;

  @Schema(description = "Overall status")
  private String status; // SUCCESS, PARTIAL_SUCCESS, FAILURE

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImportRowResult {

    @Schema(description = "Row number in file")
    private Integer rowNumber;

    @Schema(description = "Id of created question (if successful)")
    private String questionId;

    @Schema(description = "Success or error")
    private Boolean success;

    @Schema(description = "Error message if failed")
    private String errorMessage;

    @Schema(description = "Data from this row")
    private String rowData;
  }
}
