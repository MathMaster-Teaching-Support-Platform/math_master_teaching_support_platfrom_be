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
public class BatchAddTemplateMappingsRequest {

  @NotEmpty(message = "At least one template mapping must be provided")
  @Valid
  private List<AddTemplateMappingRequest> mappings;
}
