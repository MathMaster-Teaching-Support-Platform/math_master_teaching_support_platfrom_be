package com.fptu.math_master.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportQuestionsRequest {

  @NotNull(message = "File content is required")
  @Schema(description = "CSV file content (base64 encoded or raw)")
  private String fileContent;

  @Schema(description = "File format: CSV or EXCEL")
  @Builder.Default
  private String fileFormat = "CSV";

  @Schema(description = "Question bank ID to assign imported questions")
  private UUID questionBankId;

  @Schema(description = "Whether to skip invalid rows (true) or stop on first error (false)")
  @Builder.Default
  private Boolean continueOnError = true;

  @Schema(description = "Character encoding of the file (default: UTF-8)")
  @Builder.Default
  private String encoding = "UTF-8";

  public String getFileFormatUpperCase() {
    return fileFormat != null ? fileFormat.toUpperCase() : "CSV";
  }
}
