package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
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
public class BatchUpsertMatrixRowCellsRequest {

  @NotEmpty(message = "rows must not be empty")
  @Valid
  private List<RowCellsItem> rows;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RowCellsItem {

    @NotNull(message = "rowId is required")
    private UUID rowId;

    @NotEmpty(message = "cells must not be empty")
    @Valid
    private List<MatrixCellRequest> cells;
  }
}
