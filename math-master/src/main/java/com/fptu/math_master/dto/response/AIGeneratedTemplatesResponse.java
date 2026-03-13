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
public class AIGeneratedTemplatesResponse {

  private Integer totalTemplatesGenerated;
  private List<QuestionTemplateResponse> generatedTemplates;
  private String lessonName;
  private String message;
}
