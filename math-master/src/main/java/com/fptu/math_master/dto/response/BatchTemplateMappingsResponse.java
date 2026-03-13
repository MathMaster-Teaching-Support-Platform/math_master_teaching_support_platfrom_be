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
public class BatchTemplateMappingsResponse {

  private Integer totalMappingsAdded;
  private List<TemplateMappingResponse> addedMappings;
  private String message;
}
