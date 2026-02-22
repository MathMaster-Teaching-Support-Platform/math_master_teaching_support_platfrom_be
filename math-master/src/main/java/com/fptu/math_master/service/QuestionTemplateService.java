package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionTemplateService {

  QuestionTemplateResponse createQuestionTemplate(QuestionTemplateRequest request);

  QuestionTemplateResponse updateQuestionTemplate(UUID id, QuestionTemplateRequest request);

  void deleteQuestionTemplate(UUID id);

  /**
   * Test template with AI enhancement
   *
   * @param id Template ID
   * @param sampleCount Number of samples to generate
   * @param useAI Whether to use AI enhancement
   * @return Test response with AI-enhanced or regular samples
   */
  TemplateTestResponse testTemplate(UUID id, Integer sampleCount, Boolean useAI);

  /**
   * Generate a single AI-enhanced question from template
   *
   * @param id Template ID
   * @return AI-enhanced question
   */
  AIEnhancedQuestionResponse generateAIEnhancedQuestion(UUID id);

  QuestionTemplateResponse getQuestionTemplateById(UUID id);

  Page<QuestionTemplateResponse> getMyQuestionTemplates(Pageable pageable);

  Page<QuestionTemplateResponse> searchQuestionTemplates(
      QuestionType templateType,
      CognitiveLevel cognitiveLevel,
      Boolean isPublic,
      String searchTerm,
      String[] tags,
      Pageable pageable);

  QuestionTemplateResponse togglePublicStatus(UUID id);
}
