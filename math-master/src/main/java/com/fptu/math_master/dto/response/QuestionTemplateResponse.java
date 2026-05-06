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
  private UUID chapterId;
  private String chapterName;
  /**
   * Subject id of the chapter (derived from chapter -> subject). Returned so
   * the FE can hydrate the academic cascade (lớp / môn / chương) when
   * reopening an existing template for edit. Null when no chapter anchor.
   */
  private UUID subjectId;
  private String subjectName;
  private String gradeLevel;
  /**
   * Optional lesson the template was imported under. Surfaced so the FE can
   * round-trip the value on update without losing it.
   */
  private UUID lessonId;
  private QuestionType templateType;
  private TemplateVariant templateVariant;
  private Map<String, Object> templateText;
  private Map<String, Object> parameters;
  private String answerFormula;
  private String diagramTemplate;
  private String solutionStepsTemplate;
  private Map<String, Object> optionsGenerator;
  private String topic;
  private String[] constraints;
  private Map<String, Object> statementMutations;
  private CognitiveLevel cognitiveLevel;
  private java.util.List<com.fptu.math_master.enums.QuestionTag> tags;
  private Boolean isPublic;
  private TemplateStatus status;
  private Integer usageCount;
  private BigDecimal avgSuccessRate;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID questionBankId;
  private UUID canonicalQuestionId;
}
