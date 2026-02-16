package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTemplateRequest {

  @NotNull(message = "Parameters are required for testing")
  private Map<String, Object> testParameters;

  private Integer sampleCount;
}

