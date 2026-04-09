package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.enums.TemplateVariant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTemplateResponse {

  private UUID id;
  private UUID createdBy;
  private String creatorName;
  private String name;
  private String description;
  private QuestionType templateType;
  private TemplateVariant templateVariant;
  private Map<String, Object> templateText;
  private Map<String, Object> parameters;
  private String answerFormula;
  private String diagramTemplate;
  private Map<String, Object> optionsGenerator;
  private String topic;
  private String[] constraints;
  private CognitiveLevel cognitiveLevel;
  private String[] tags;
  private Boolean isPublic;
  private TemplateStatus status;
  private Integer usageCount;
  private BigDecimal avgSuccessRate;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID questionBankId;
  private UUID canonicalQuestionId;
}
