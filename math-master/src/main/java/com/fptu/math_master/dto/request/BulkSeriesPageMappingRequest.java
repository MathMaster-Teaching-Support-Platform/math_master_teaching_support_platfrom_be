package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSeriesPageMappingRequest {

  @NotEmpty(message = "mappings must not be empty")
  @Valid
  private List<SeriesPageMappingItem> mappings;
}

