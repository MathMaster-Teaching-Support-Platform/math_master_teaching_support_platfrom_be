package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.ExtractParametersRequest;
import com.fptu.math_master.dto.request.GenerateParametersRequest;
import com.fptu.math_master.dto.request.SetClausePointsRequest;
import com.fptu.math_master.dto.request.UpdateParametersRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.ExtractParametersResponse;
import com.fptu.math_master.dto.response.GenerateParametersResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.QuestionTemplate;
import java.util.UUID;

/** Service for enhancing questions using Gemini AI (Google AI Studio) */
public interface AIEnhancementService {

  /**
   * Enhance a question using Gemini AI
   *
   * @param request The enhancement request with raw question data
   * @return Enhanced question with better wording, distractors, and explanations
   */
  AIEnhancedQuestionResponse enhanceQuestion(AIEnhancementRequest request);

  /**
   * Validate AI-generated content
   *
   * @param request Original request
   * @param response AI-generated response
   * @return true if valid, false otherwise
   */
  boolean validateAIOutput(AIEnhancementRequest request, AIEnhancedQuestionResponse response);

  /**
   * Test AI enhancement with a sample
   *
   * @param request The enhancement request
   * @return Enhanced question or fallback with error details
   */
  AIEnhancedQuestionResponse testEnhancement(AIEnhancementRequest request);

  /**
   * Fully generate a question sample from a template using LLM. LLM will choose parameters,
   * generate question text, options, answer and explanation.
   *
   * @param template The question template
   * @param sampleIndex Index of the sample (for variety)
   * @return Generated question sample
   */
  GeneratedQuestionSample generateQuestion(QuestionTemplate template, int sampleIndex);

  /**
   * Generate a question by keeping semantic structure from a canonical teacher-authored question.
   * The result is still compatible with existing matrix-based flows.
   */
  GeneratedQuestionSample generateQuestionFromCanonical(
      CanonicalQuestion canonicalQuestion, QuestionTemplate template, int sampleIndex);

  // =========================================================================
  // FEATURE 1: AI Auto-Extract Parameters From Question Text
  // =========================================================================

  /**
   * AI reads all content fields of a question template and identifies which
   * numbers are CHANGEABLE parameters (should become {{a}} {{b}} {{c}})
   * and which are FIXED (structural exponents, derived values, constants).
   *
   * @param templateId The template to analyze
   * @param request    Content fields submitted by teacher
   * @return Suggestions with changeable params, fixed values, and the auto-converted template text
   */
  ExtractParametersResponse extractParameters(UUID templateId, ExtractParametersRequest request);

  // =========================================================================
  // FEATURE 2: AI Generates Parameter Values (Replaces Backend Random)
  // =========================================================================

  /**
   * AI reads all content fields (template text, formula, steps, diagram, options/clauses,
   * and existing samples) and generates a valid parameter combination that satisfies
   * all math constraints detected from the formula.
   *
   * @param templateId The template for which to generate parameters
   * @param request    All content fields needed for constraint analysis
   * @return Generated parameter values + per-param constraint explanation + combined constraints
   */
  GenerateParametersResponse generateParameters(UUID templateId, GenerateParametersRequest request);

  /**
   * Teacher adjusts AI-generated parameters via a plain-text command.
   * AI re-generates values satisfying all existing constraints PLUS the new requirement.
   *
   * @param templateId The template for which to update parameters
   * @param request    Current parameter values, constraint text, and teacher command
   * @return Updated parameters + updated constraint text
   */
  GenerateParametersResponse updateParameters(UUID templateId, UpdateParametersRequest request);

  // =========================================================================
  // FEATURE 4: Set Overdrive Points per Clause (TF Only)
  // =========================================================================

  /**
   * Teacher sets how many points each TF clause (A/B/C/D) is worth.
   * Validates that sum(clause_points) == total_point.
   * Persists overdrive_point per clause into the question's options JSON.
   *
   * @param questionId The TRUE_FALSE question to update
   * @param request    Total point and per-clause point distribution
   */
  void setClausePoints(UUID questionId, SetClausePointsRequest request);

  // =========================================================================
  // FEATURE 5: TF Clause Content Validation for Matrix Selection
  // =========================================================================

  /**
   * AI validates whether a TF clause content matches the required chapter and
   * cognitive level. Used after AI generates clauses during matrix-based question
   * selection to ensure chapter/level alignment.
   *
   * @param clauseText     The clause statement text to validate
   * @param chapterName    The required chapter name
   * @param cognitiveLevel The required cognitive level
   * @return true if the clause matches both chapter and cognitive level, false otherwise
   */
  boolean validateClauseForMatrix(String clauseText, String chapterName, String cognitiveLevel);
}
