package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AIGenerateTemplatesRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.request.GenerateTemplateQuestionsRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.AIGeneratedTemplatesResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
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

  GeneratedQuestionsBatchResponse generateQuestionsFromTemplate(
      UUID id, GenerateTemplateQuestionsRequest request);

    GeneratedQuestionsBatchResponse generateQuestionsFromCanonical(
      UUID canonicalQuestionId, GenerateCanonicalQuestionsRequest request);

  /**
   * AI generates one or more question templates based on lesson content
   *
   * @param request AIGenerateTemplatesRequest with lessonId and optional templateCount
   * @return Response with list of generated templates
   */
  AIGeneratedTemplatesResponse aiGenerateTemplates(AIGenerateTemplatesRequest request);

  QuestionTemplateResponse getQuestionTemplateById(UUID id);

  Page<QuestionTemplateResponse> getMyQuestionTemplates(Pageable pageable);

  Page<QuestionTemplateResponse> getMyQuestionTemplatesFiltered(
      String search, TemplateStatus status, Pageable pageable);

  Page<QuestionTemplateResponse> searchQuestionTemplates(
      QuestionType templateType,
      CognitiveLevel cognitiveLevel,
      Boolean isPublic,
      String searchTerm,
      String[] tags,
      Pageable pageable);

  QuestionTemplateResponse togglePublicStatus(UUID id);

  /** Promote a DRAFT template to PUBLISHED so it can be used for question generation. */
  QuestionTemplateResponse publishTemplate(UUID id);

  /** Move a PUBLISHED template to ARCHIVED so it can no longer be used for generation. */
  QuestionTemplateResponse archiveTemplate(UUID id);
}
