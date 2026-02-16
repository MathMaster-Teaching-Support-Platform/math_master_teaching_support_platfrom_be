package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;

/**
 * Service for enhancing questions using Ollama AI (Llama 3)
 */
public interface AIEnhancementService {

    /**
     * Enhance a question using Ollama AI
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
}

