package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalQuestionResponse {

  private UUID id;
  private UUID createdBy;
  private String creatorName;
  private String title;
  private String problemText;
  private String solutionSteps;
  private String diagramDefinition;
  private QuestionType problemType;
  private CognitiveLevel cognitiveLevel;
  private Instant createdAt;
  private Instant updatedAt;
}