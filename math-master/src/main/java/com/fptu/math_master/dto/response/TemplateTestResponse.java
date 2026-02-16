package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTestResponse {

  private UUID templateId;
  private String templateName;
  private List<GeneratedQuestionSample> samples;
  private Boolean isValid;
  private List<String> validationErrors;
}

