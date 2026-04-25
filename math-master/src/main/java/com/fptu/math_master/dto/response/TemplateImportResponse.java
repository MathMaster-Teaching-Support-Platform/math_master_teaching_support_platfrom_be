package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateImportResponse {

  /** Extracted raw text from file */
  String extractedText;

  /** AI-detected question structure */
  QuestionStructureAnalysis analysis;

  /** Suggested template draft for teacher review */
  TemplateDraft suggestedTemplate;

  /** AI confidence score (0.0 - 1.0) */
  Double confidenceScore;

  /** Warnings or notes for teacher */
  List<String> warnings;

  /** Whether AI analysis was successful */
  Boolean analysisSuccessful;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class QuestionStructureAnalysis {
    /** Detected question type */
    QuestionType detectedType;

    /** Detected repeating patterns */
    List<String> detectedPatterns;

    /** Suggested placeholders */
    List<PlaceholderSuggestion> placeholderSuggestions;

    /** Detected formulas or calculations */
    List<String> detectedFormulas;

    /** Mathematical structure description (e.g., "ax + b = c") */
    String mathematicalStructure;

    /** Detected content language */
    String detectedLanguage;

    /** Sample questions found */
    List<String> sampleQuestions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class PlaceholderSuggestion {
    /** Suggested variable name */
    String variableName;

    /** Detected type (integer, decimal, text) */
    String type;

    /** Suggested min value */
    Object minValue;

    /** Suggested max value */
    Object maxValue;

    /** Example values detected */
    List<String> exampleValues;

    /** Description of what this variable represents */
    String description;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TemplateDraft {
    /** Suggested template name */
    String name;

    /** Suggested description */
    String description;

    /** Template type */
    QuestionType templateType;

    /** Template text with placeholders */
    Map<String, String> templateText;

    /** Parameter definitions */
    Map<String, Object> parameters;

    /** Suggested answer formula */
    String answerFormula;

    /** Options generator config */
    Map<String, Object> optionsGenerator;

    /** Suggested cognitive level */
    CognitiveLevel cognitiveLevel;

    /** Suggested tags */
    java.util.List<com.fptu.math_master.enums.QuestionTag> tags;
  }
}
