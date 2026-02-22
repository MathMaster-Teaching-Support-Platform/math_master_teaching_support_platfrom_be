package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.QuestionTemplate;

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
   * Fully generate a question sample from a template using LLM.
   * LLM will choose parameters, generate question text, options, answer and explanation.
   *
   * @param template The question template
   * @param sampleIndex Index of the sample (for variety)
   * @return Generated question sample
   */
  GeneratedQuestionSample generateQuestion(QuestionTemplate template, int sampleIndex);
}
