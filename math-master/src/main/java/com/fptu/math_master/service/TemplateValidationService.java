package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.request.UpdateQuestionTemplateRequest;
import com.fptu.math_master.dto.response.TemplateValidationResponse;

/**
 * FR-TPL-002: Template Validation Service
 * Provides real-time validation for question templates
 */
public interface TemplateValidationService {

  /**
   * Validate template for creation
   */
  TemplateValidationResponse validateTemplate(QuestionTemplateRequest request);

  /**
   * Validate template for update
   */
  TemplateValidationResponse validateTemplateUpdate(UpdateQuestionTemplateRequest request);

  /**
   * Quick validation - only critical errors
   */
  TemplateValidationResponse quickValidate(UpdateQuestionTemplateRequest request);
}

