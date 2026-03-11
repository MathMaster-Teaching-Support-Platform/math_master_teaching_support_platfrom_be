package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for GET /exam-matrices/{matrixId}/cells/{cellId}/templates Returns ranked list
 * of matching question templates for a matrix cell.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingTemplatesResponse {

  /** Summary of matrix requirements used for filtering (for UI display). */
  private MappingRequirementsInfo mappingRequirements;

  /** Total number of templates found matching the criteria. */
  private int totalTemplatesFound;

  /** Ranked list of matching templates (ordered by relevance descending). */
  private List<TemplateItem> templates;

  /** Hint message shown when no templates are found or as general guidance. */
  private String hint;

  // ─────────────────────────────────────────────────────────────────────
  // Nested: MappingRequirementsInfo
  // ─────────────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MappingRequirementsInfo {
    private UUID matrixId;
    private CognitiveLevel cognitiveLevel;
    private Integer questionCount;
  }

  // ─────────────────────────────────────────────────────────────────────
  // Nested: TemplateItem (lightweight – no heavy fields like parameters/formula/answerFormula)
  // ─────────────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TemplateItem {

    private UUID templateId;
    private String name;
    private String description;
    private QuestionType templateType;
    private CognitiveLevel cognitiveLevel;
    private String[] tags;

    /** Whether this template was created by the requesting teacher. */
    private boolean mine;

    private Boolean isPublic;
    private UUID createdBy;
    private String createdByName;

    private Integer usageCount;
    private BigDecimal avgSuccessRate;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Relevance score used for ranking (higher = better match). Computed at service layer, not
     * persisted.
     */
    private int relevanceScore;
  }
}
