package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AutoBlueprintRequest;
import com.fptu.math_master.dto.response.BlueprintFromRealQuestionResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import java.util.List;
import java.util.Map;

/**
 * Builds and exercises Blueprints. The two creation methods both end here:
 *
 * <ul>
 *   <li><b>Method 1</b> — {@link #blueprintFromRealQuestion(AutoBlueprintRequest)}
 *       takes a complete real-valued question and returns a Blueprint draft.</li>
 *   <li><b>Method 2</b> — handled by the existing {@code QuestionTemplateService}
 *       create endpoint with the new parameter shape.</li>
 * </ul>
 *
 * Generation is a third concern: {@link #selectValueSets(QuestionTemplate, int, java.util.List, String)}
 * runs the constraint-aware AI selector. Per-set substitution + answer evaluation
 * is left to {@code AIEnhancementService}.
 */
public interface BlueprintService {

  /**
   * Method 1 reverse-templating. The teacher submits a complete question with real
   * numbers; the AI returns a Blueprint draft (template text + parameters with text
   * constraints + answer formula + diff). Nothing is persisted.
   */
  BlueprintFromRealQuestionResponse blueprintFromRealQuestion(AutoBlueprintRequest request);

  /**
   * Generation-time value selection. Reads the Blueprint's parameters and
   * globalConstraints, asks the AI for {@code count} distinct value sets, validates
   * each against simple programmatic guardrails extracted from {@code constraintText}.
   *
   * @param template      the persisted Blueprint
   * @param count         how many sets are needed
   * @param alreadyUsed   tuples already taken (for distinctness)
   * @param distinctnessHint optional free-text hint forwarded into the prompt
   * @return up to {@code count} valid value sets; may be shorter if the constraint
   *     system is over-constrained.
   */
  List<Map<String, Object>> selectValueSets(
      QuestionTemplate template,
      int count,
      List<Map<String, Object>> alreadyUsed,
      String distinctnessHint);

  /** Hash of the prompt used by {@link #selectValueSets}, for audit stamps. */
  String selectionPromptVersion();

  /** Hash of the prompt used by {@link #blueprintFromRealQuestion}, for audit stamps. */
  String reversePromptVersion();
}
