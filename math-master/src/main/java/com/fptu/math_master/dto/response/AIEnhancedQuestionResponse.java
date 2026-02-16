package com.fptu.math_master.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIEnhancedQuestionResponse {

    /**
     * Enhanced question text with better wording
     */
    String enhancedQuestionText;

    /**
     * Enhanced options with better distractors (for MCQ)
     */
    Map<String, String> enhancedOptions;

    /**
     * The correct answer option key (e.g., "A", "B", "C", "D")
     */
    String correctAnswerKey;

    /**
     * Detailed explanation/solution steps
     */
    String explanation;

    /**
     * Alternative solution methods (optional)
     */
    List<String> alternativeSolutions;

    /**
     * Common mistakes explanation for each distractor
     */
    Map<String, String> distractorExplanations;

    /**
     * Whether AI enhancement was successful
     */
    boolean enhanced;

    /**
     * Validation status
     */
    boolean isValid;

    /**
     * Validation errors if any
     */
    List<String> validationErrors;

    /**
     * Original (fallback) question text
     */
    String originalQuestionText;

    /**
     * Original (fallback) options
     */
    Map<String, String> originalOptions;
}

