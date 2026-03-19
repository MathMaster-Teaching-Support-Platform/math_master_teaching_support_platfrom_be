package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.request.UpdateQuestionTemplateRequest;
import com.fptu.math_master.dto.response.TemplateValidationResponse;
import com.fptu.math_master.dto.response.TemplateValidationResponse.IssueSeverity;
import com.fptu.math_master.dto.response.TemplateValidationResponse.ValidationIssue;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.service.TemplateValidationService;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FR-TPL-002: Template Validation Service Implementation Comprehensive validation with
 * ERROR/WARNING/INFO severity levels
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateValidationServiceImpl implements TemplateValidationService {

  private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

  @Override
  public TemplateValidationResponse validateTemplate(QuestionTemplateRequest request) {
    List<ValidationIssue> errors = new ArrayList<>();
    List<ValidationIssue> warnings = new ArrayList<>();
    List<ValidationIssue> info = new ArrayList<>();

    // Validate all aspects
    validateTemplateText(request.getTemplateText(), request.getParameters(), errors, warnings);
    validateParameters(request.getParameters(), errors, warnings, info);
    validateAnswerFormula(request.getAnswerFormula(), request.getParameters(), errors, warnings);
    validateConstraints(request.getConstraints(), request.getParameters(), errors, warnings);
    validateDifficultyRules(request.getDifficultyRules(), errors, warnings);
    validateOptionsGenerator(
        request.getOptionsGenerator(), request.getTemplateType(), errors, warnings, info);
    validateTags(request.getTags(), warnings, info);

    return buildResponse(errors, warnings, info);
  }

  @Override
  public TemplateValidationResponse validateTemplateUpdate(UpdateQuestionTemplateRequest request) {
    List<ValidationIssue> errors = new ArrayList<>();
    List<ValidationIssue> warnings = new ArrayList<>();
    List<ValidationIssue> info = new ArrayList<>();

    // Validate all aspects
    validateTemplateText(request.getTemplateText(), request.getParameters(), errors, warnings);
    validateParameters(request.getParameters(), errors, warnings, info);
    validateAnswerFormula(request.getAnswerFormula(), request.getParameters(), errors, warnings);
    validateConstraints(request.getConstraints(), request.getParameters(), errors, warnings);
    validateDifficultyRules(request.getDifficultyRules(), errors, warnings);
    validateOptionsGenerator(
        request.getOptionsGenerator(), request.getTemplateType(), errors, warnings, info);
    validateTags(request.getTags(), warnings, info);

    return buildResponse(errors, warnings, info);
  }

  @Override
  public TemplateValidationResponse quickValidate(UpdateQuestionTemplateRequest request) {
    List<ValidationIssue> errors = new ArrayList<>();

    // Only critical validations
    if (request.getTemplateText() == null || request.getTemplateText().isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("TEMPLATE_TEXT")
              .field("templateText")
              .message("Template text is required")
              .severity(IssueSeverity.ERROR)
              .build());
    }

    if (request.getParameters() == null || request.getParameters().isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("PARAMETERS")
              .field("parameters")
              .message("At least one parameter is required")
              .severity(IssueSeverity.ERROR)
              .build());
    }

    if (request.getAnswerFormula() == null || request.getAnswerFormula().trim().isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("FORMULA")
              .field("answerFormula")
              .message("Answer formula is required")
              .severity(IssueSeverity.ERROR)
              .build());
    }

    return TemplateValidationResponse.builder()
        .isValid(errors.isEmpty())
        .errorCount(errors.size())
        .warningCount(0)
        .errors(errors)
        .warnings(new ArrayList<>())
        .info(new ArrayList<>())
        .build();
  }

  // ===== Validation Methods =====

  @SuppressWarnings("unchecked")
  private void validateTemplateText(
      Map<String, Object> templateText,
      Map<String, Object> parameters,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings) {

    if (templateText == null || templateText.isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("TEMPLATE_TEXT")
              .field("templateText")
              .message("Template text is required")
              .severity(IssueSeverity.ERROR)
              .build());
      return;
    }

    // Extract all placeholders from all language variants
    Set<String> allPlaceholders = new HashSet<>();
    Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    for (Map.Entry<String, Object> entry : templateText.entrySet()) {
      String lang = entry.getKey();
      String text = entry.getValue().toString();

      if (text == null || text.trim().isEmpty()) {
        warnings.add(
            ValidationIssue.builder()
                .category("TEMPLATE_TEXT")
                .field("templateText." + lang)
                .message("Template text for language '" + lang + "' is empty")
                .severity(IssueSeverity.WARNING)
                .build());
        continue;
      }

      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
        allPlaceholders.add(matcher.group(1));
      }

      // Check for proper math symbols
      if (text.contains("+") || text.contains("-") || text.contains("*") || text.contains("/")) {
        // Check if using proper minus sign (optional enhancement)
        if (text.contains("−") && text.contains("-")) {
          warnings.add(
              ValidationIssue.builder()
                  .category("TEMPLATE_TEXT")
                  .field("templateText." + lang)
                  .message("Mixed usage of minus sign '−' and hyphen '-'. Consider standardizing.")
                  .severity(IssueSeverity.WARNING)
                  .suggestion("Use consistent minus sign format")
                  .build());
        }
      }
    }

    // Validate all placeholders have parameter definitions
    if (parameters != null) {
      for (String placeholder : allPlaceholders) {
        if (!parameters.containsKey(placeholder)) {
          errors.add(
              ValidationIssue.builder()
                  .category("TEMPLATE_TEXT")
                  .field("templateText")
                  .message("Placeholder {{" + placeholder + "}} not defined in parameters")
                  .severity(IssueSeverity.ERROR)
                  .suggestion("Add parameter definition for '" + placeholder + "'")
                  .build());
        }
      }

      // Warn about unused parameters
      for (String paramName : parameters.keySet()) {
        if (!allPlaceholders.contains(paramName)) {
          warnings.add(
              ValidationIssue.builder()
                  .category("PARAMETERS")
                  .field("parameters." + paramName)
                  .message("Parameter '" + paramName + "' is defined but not used in template text")
                  .severity(IssueSeverity.WARNING)
                  .suggestion("Remove unused parameter or add {{" + paramName + "}} to template")
                  .build());
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void validateParameters(
      Map<String, Object> parameters,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings,
      List<ValidationIssue> info) {

    if (parameters == null || parameters.isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("PARAMETERS")
              .field("parameters")
              .message("At least one parameter is required")
              .severity(IssueSeverity.ERROR)
              .build());
      return;
    }

    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String paramName = entry.getKey();
      Object paramDefObj = entry.getValue();

      if (!(paramDefObj instanceof Map)) {
        errors.add(
            ValidationIssue.builder()
                .category("PARAMETERS")
                .field("parameters." + paramName)
                .message("Parameter '" + paramName + "' must be an object with type, min, max")
                .severity(IssueSeverity.ERROR)
                .build());
        continue;
      }

      Map<String, Object> paramDef = (Map<String, Object>) paramDefObj;

      // Validate required fields
      if (!paramDef.containsKey("type")) {
        errors.add(
            ValidationIssue.builder()
                .category("PARAMETERS")
                .field("parameters." + paramName + ".type")
                .message("Parameter '" + paramName + "' must have 'type' field")
                .severity(IssueSeverity.ERROR)
                .suggestion("Add type: 'integer', 'decimal', or 'text'")
                .build());
      } else {
        String type = paramDef.get("type").toString();
        if (!type.equals("integer") && !type.equals("decimal") && !type.equals("text")) {
          errors.add(
              ValidationIssue.builder()
                  .category("PARAMETERS")
                  .field("parameters." + paramName + ".type")
                  .message(
                      "Parameter '"
                          + paramName
                          + "' has invalid type '"
                          + type
                          + "'. Must be: integer, decimal, or text")
                  .severity(IssueSeverity.ERROR)
                  .build());
        }
      }

      if (!paramDef.containsKey("min")) {
        warnings.add(
            ValidationIssue.builder()
                .category("PARAMETERS")
                .field("parameters." + paramName + ".min")
                .message("Parameter '" + paramName + "' should have 'min' value")
                .severity(IssueSeverity.WARNING)
                .suggestion("Add min value for better control")
                .build());
      }

      if (!paramDef.containsKey("max")) {
        warnings.add(
            ValidationIssue.builder()
                .category("PARAMETERS")
                .field("parameters." + paramName + ".max")
                .message("Parameter '" + paramName + "' should have 'max' value")
                .severity(IssueSeverity.WARNING)
                .suggestion("Add max value for better control")
                .build());
      }

      // Validate min < max
      if (paramDef.containsKey("min") && paramDef.containsKey("max")) {
        try {
          double min = ((Number) paramDef.get("min")).doubleValue();
          double max = ((Number) paramDef.get("max")).doubleValue();
          if (min >= max) {
            errors.add(
                ValidationIssue.builder()
                    .category("PARAMETERS")
                    .field("parameters." + paramName)
                    .message(
                        "Parameter '"
                            + paramName
                            + "' min value ("
                            + min
                            + ") must be less than max ("
                            + max
                            + ")")
                    .severity(IssueSeverity.ERROR)
                    .build());
          }

          // Check for reasonable ranges
          if (max - min > 10000) {
            warnings.add(
                ValidationIssue.builder()
                    .category("PARAMETERS")
                    .field("parameters." + paramName)
                    .message(
                        "Parameter '"
                            + paramName
                            + "' has very large range ("
                            + (max - min)
                            + "). Consider reducing for better question quality.")
                    .severity(IssueSeverity.WARNING)
                    .build());
          }
        } catch (Exception e) {
          errors.add(
              ValidationIssue.builder()
                  .category("PARAMETERS")
                  .field("parameters." + paramName)
                  .message("Parameter '" + paramName + "' min/max values must be numeric")
                  .severity(IssueSeverity.ERROR)
                  .build());
        }
      }

      // Check for exclude array
      if (paramDef.containsKey("exclude")) {
        Object excludeObj = paramDef.get("exclude");
        if (excludeObj instanceof List) {
          List<?> excludeList = (List<?>) excludeObj;
          if (!excludeList.isEmpty()) {
            info.add(
                ValidationIssue.builder()
                    .category("PARAMETERS")
                    .field("parameters." + paramName + ".exclude")
                    .message(
                        "Parameter '"
                            + paramName
                            + "' excludes "
                            + excludeList.size()
                            + " value(s)")
                    .severity(IssueSeverity.INFO)
                    .build());
          }
        }
      }

      // Check for step
      if (paramDef.containsKey("step")) {
        info.add(
            ValidationIssue.builder()
                .category("PARAMETERS")
                .field("parameters." + paramName + ".step")
                .message("Parameter '" + paramName + "' uses step increment")
                .severity(IssueSeverity.INFO)
                .build());
      }
    }
  }

  private void validateAnswerFormula(
      String answerFormula,
      Map<String, Object> parameters,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings) {

    if (answerFormula == null || answerFormula.trim().isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("FORMULA")
              .field("answerFormula")
              .message("Answer formula is required")
              .severity(IssueSeverity.ERROR)
              .build());
      return;
    }

    // Check for proper operator precedence
    if (answerFormula.contains("+") || answerFormula.contains("-")) {
      if (answerFormula.contains("*") || answerFormula.contains("/")) {
        if (!answerFormula.contains("(")) {
          warnings.add(
              ValidationIssue.builder()
                  .category("FORMULA")
                  .field("answerFormula")
                  .message(
                      "Formula mixes addition/subtraction with multiplication/division. Consider using parentheses for clarity.")
                  .severity(IssueSeverity.WARNING)
                  .suggestion("Add parentheses: e.g., (a + b) * c or a + (b * c)")
                  .build());
        }
      }
    }

    // Check for division by zero risk
    if (answerFormula.contains("/")) {
      Pattern divPattern = Pattern.compile("/(\\s*\\w+)");
      Matcher matcher = divPattern.matcher(answerFormula);
      while (matcher.find()) {
        String divisor = matcher.group(1).trim();
        warnings.add(
            ValidationIssue.builder()
                .category("FORMULA")
                .field("answerFormula")
                .message(
                    "Formula divides by '"
                        + divisor
                        + "'. Ensure constraint '"
                        + divisor
                        + " != 0' exists.")
                .severity(IssueSeverity.WARNING)
                .suggestion("Add constraint: " + divisor + " != 0")
                .build());
      }
    }

    // Try to evaluate formula with test values
    if (parameters != null && !parameters.isEmpty()) {
      try {
        ScriptEngine engine = getJavaScriptEngine();
        if (engine == null) {
          warnings.add(
              ValidationIssue.builder()
                  .category("FORMULA")
                  .field("answerFormula")
                  .message(
                      "Cannot validate formula: JavaScript engine not available. Add GraalVM JavaScript dependency.")
                  .severity(IssueSeverity.WARNING)
                  .build());
          return;
        }

        // Set test values for all parameters
        for (String paramName : parameters.keySet()) {
          engine.put(paramName, 5); // Use 5 as safe test value
        }

        Object result = engine.eval(answerFormula);
        if (result == null) {
          errors.add(
              ValidationIssue.builder()
                  .category("FORMULA")
                  .field("answerFormula")
                  .message("Formula evaluation returned null")
                  .severity(IssueSeverity.ERROR)
                  .build());
        } else if (!(result instanceof Number)) {
          warnings.add(
              ValidationIssue.builder()
                  .category("FORMULA")
                  .field("answerFormula")
                  .message(
                      "Formula result is not a number (got: "
                          + result.getClass().getSimpleName()
                          + ")")
                  .severity(IssueSeverity.WARNING)
                  .build());
        }

      } catch (Exception e) {
        errors.add(
            ValidationIssue.builder()
                .category("FORMULA")
                .field("answerFormula")
                .message("Invalid formula syntax: " + e.getMessage())
                .severity(IssueSeverity.ERROR)
                .suggestion("Check formula syntax and parameter names")
                .build());
      }
    }

    // Check for undefined parameters in formula
    if (parameters != null) {
      Pattern paramPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
      Matcher matcher = paramPattern.matcher(answerFormula);
      Set<String> usedParams = new HashSet<>();
      while (matcher.find()) {
        String param = matcher.group(1);
        // Skip JavaScript keywords and Math functions
        if (!isJavaScriptKeyword(param) && !param.equals("Math") && !param.equals("answer")) {
          usedParams.add(param);
        }
      }

      for (String param : usedParams) {
        if (!parameters.containsKey(param)) {
          errors.add(
              ValidationIssue.builder()
                  .category("FORMULA")
                  .field("answerFormula")
                  .message("Formula uses undefined parameter '" + param + "'")
                  .severity(IssueSeverity.ERROR)
                  .suggestion("Add parameter definition for '" + param + "' or fix formula")
                  .build());
        }
      }
    }
  }

  private void validateConstraints(
      String[] constraints,
      Map<String, Object> parameters,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings) {

    if (constraints == null || constraints.length == 0) {
      warnings.add(
          ValidationIssue.builder()
              .category("CONSTRAINTS")
              .field("constraints")
              .message(
                  "No constraints defined. Consider adding constraints for better question quality.")
              .severity(IssueSeverity.WARNING)
              .suggestion("Add constraints like 'a != 0', 'answer > 0', 'answer % 1 == 0'")
              .build());
      return;
    }

    for (int i = 0; i < constraints.length; i++) {
      String constraint = constraints[i];
      if (constraint == null || constraint.trim().isEmpty()) {
        warnings.add(
            ValidationIssue.builder()
                .category("CONSTRAINTS")
                .field("constraints[" + i + "]")
                .message("Empty constraint at index " + i)
                .severity(IssueSeverity.WARNING)
                .build());
        continue;
      }

      // Try to validate constraint syntax
      try {
        ScriptEngine engine = getJavaScriptEngine();
        if (engine != null && parameters != null) {
          // Set test values
          for (String paramName : parameters.keySet()) {
            engine.put(paramName, 5);
          }
          engine.put("answer", 10);

          Object result = engine.eval(constraint);
          if (!(result instanceof Boolean)) {
            warnings.add(
                ValidationIssue.builder()
                    .category("CONSTRAINTS")
                    .field("constraints[" + i + "]")
                    .message("Constraint '" + constraint + "' does not return boolean")
                    .severity(IssueSeverity.WARNING)
                    .build());
          }
        }
      } catch (Exception e) {
        errors.add(
            ValidationIssue.builder()
                .category("CONSTRAINTS")
                .field("constraints[" + i + "]")
                .message("Invalid constraint syntax: " + constraint + " - " + e.getMessage())
                .severity(IssueSeverity.ERROR)
                .build());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void validateDifficultyRules(
      Map<String, Object> difficultyRules,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings) {

    if (difficultyRules == null || difficultyRules.isEmpty()) {
      errors.add(
          ValidationIssue.builder()
              .category("DIFFICULTY_RULES")
              .field("difficultyRules")
              .message("Difficulty rules are required")
              .severity(IssueSeverity.ERROR)
              .suggestion("Add rules for 'easy', 'medium', and 'hard' difficulties")
              .build());
      return;
    }

    // Check for standard difficulty levels
    boolean hasEasy = difficultyRules.containsKey("easy");
    boolean hasMedium = difficultyRules.containsKey("medium");
    boolean hasHard = difficultyRules.containsKey("hard");

    if (!hasEasy || !hasMedium || !hasHard) {
      warnings.add(
          ValidationIssue.builder()
              .category("DIFFICULTY_RULES")
              .field("difficultyRules")
              .message(
                  "Missing standard difficulty levels. Found: "
                      + difficultyRules.keySet()
                      + ". Expected: easy, medium, hard")
              .severity(IssueSeverity.WARNING)
              .suggestion("Add all three difficulty levels for completeness")
              .build());
    }

    // Check for overlapping or non-exclusive rules
    List<String> ruleStrings = new ArrayList<>();
    for (Map.Entry<String, Object> entry : difficultyRules.entrySet()) {
      String level = entry.getKey();
      String rule = entry.getValue().toString();

      if (rule == null || rule.trim().isEmpty()) {
        errors.add(
            ValidationIssue.builder()
                .category("DIFFICULTY_RULES")
                .field("difficultyRules." + level)
                .message("Difficulty rule for '" + level + "' is empty")
                .severity(IssueSeverity.ERROR)
                .build());
        continue;
      }

      ruleStrings.add(rule);

      // Check if rule is always true or always false
      if (rule.equalsIgnoreCase("true")) {
        warnings.add(
            ValidationIssue.builder()
                .category("DIFFICULTY_RULES")
                .field("difficultyRules." + level)
                .message("Difficulty rule for '" + level + "' is always true")
                .severity(IssueSeverity.WARNING)
                .suggestion("Add specific conditions based on parameters")
                .build());
      }
      if (rule.equalsIgnoreCase("false")) {
        warnings.add(
            ValidationIssue.builder()
                .category("DIFFICULTY_RULES")
                .field("difficultyRules." + level)
                .message(
                    "Difficulty rule for '"
                        + level
                        + "' is always false - this level will never match")
                .severity(IssueSeverity.WARNING)
                .build());
      }
    }

    // Warn about potentially overlapping rules
    if (ruleStrings.size() >= 2) {
      boolean mayOverlap = false;
      for (int i = 0; i < ruleStrings.size(); i++) {
        for (int j = i + 1; j < ruleStrings.size(); j++) {
          String rule1 = ruleStrings.get(i);
          String rule2 = ruleStrings.get(j);
          // Simple heuristic: if rules share variables but different operators, might overlap
          if (containsSimilarVariables(rule1, rule2)) {
            mayOverlap = true;
            break;
          }
        }
      }

      if (mayOverlap) {
        warnings.add(
            ValidationIssue.builder()
                .category("DIFFICULTY_RULES")
                .field("difficultyRules")
                .message(
                    "Difficulty rules may overlap. Ensure rules are mutually exclusive or define priority.")
                .severity(IssueSeverity.WARNING)
                .suggestion(
                    "Use exclusive conditions like: easy: 'x <= 3', medium: 'x > 3 AND x <= 7', hard: 'x > 7'")
                .build());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void validateOptionsGenerator(
      Map<String, Object> optionsGenerator,
      QuestionType templateType,
      List<ValidationIssue> errors,
      List<ValidationIssue> warnings,
      List<ValidationIssue> info) {

    if (templateType == QuestionType.MULTIPLE_CHOICE) {
      if (optionsGenerator == null || optionsGenerator.isEmpty()) {
        errors.add(
            ValidationIssue.builder()
                .category("OPTIONS_GENERATOR")
                .field("optionsGenerator")
                .message("Options generator is required for MULTIPLE_CHOICE question type")
                .severity(IssueSeverity.ERROR)
                .suggestion("Add optionsGenerator with count and distractors configuration")
                .build());
        return;
      }

      // Check for required fields
      if (!optionsGenerator.containsKey("count")) {
        warnings.add(
            ValidationIssue.builder()
                .category("OPTIONS_GENERATOR")
                .field("optionsGenerator.count")
                .message("Options count not specified, will default to 4")
                .severity(IssueSeverity.WARNING)
                .build());
      } else {
        int count = ((Number) optionsGenerator.get("count")).intValue();
        if (count < 2 || count > 6) {
          warnings.add(
              ValidationIssue.builder()
                  .category("OPTIONS_GENERATOR")
                  .field("optionsGenerator.count")
                  .message("Options count (" + count + ") should be between 2 and 6")
                  .severity(IssueSeverity.WARNING)
                  .build());
        }
      }

      // Check for distractors
      if (!optionsGenerator.containsKey("distractors")) {
        warnings.add(
            ValidationIssue.builder()
                .category("OPTIONS_GENERATOR")
                .field("optionsGenerator.distractors")
                .message("No distractor strategies defined. Consider adding common student errors.")
                .severity(IssueSeverity.WARNING)
                .suggestion(
                    "Add distractors like 'Forgot to divide by a', 'Sign error', 'Calculation mistake'")
                .build());
      } else {
        Object distractorsObj = optionsGenerator.get("distractors");
        if (distractorsObj instanceof List) {
          List<?> distractors = (List<?>) distractorsObj;
          if (distractors.isEmpty()) {
            warnings.add(
                ValidationIssue.builder()
                    .category("OPTIONS_GENERATOR")
                    .field("optionsGenerator.distractors")
                    .message("Distractor list is empty")
                    .severity(IssueSeverity.WARNING)
                    .build());
          } else {
            info.add(
                ValidationIssue.builder()
                    .category("OPTIONS_GENERATOR")
                    .field("optionsGenerator.distractors")
                    .message("Defined " + distractors.size() + " distractor strategy(ies)")
                    .severity(IssueSeverity.INFO)
                    .build());
          }
        }
      }

      // Ensure numeric options for math questions
      if (optionsGenerator.containsKey("type")) {
        String type = optionsGenerator.get("type").toString();
        if (!type.equals("numeric") && !type.equals("around_answer")) {
          warnings.add(
              ValidationIssue.builder()
                  .category("OPTIONS_GENERATOR")
                  .field("optionsGenerator.type")
                  .message(
                      "For math questions, consider using 'numeric' or 'around_answer' type for better options")
                  .severity(IssueSeverity.WARNING)
                  .build());
        }
      }
    } else if (optionsGenerator != null && !optionsGenerator.isEmpty()) {
      warnings.add(
          ValidationIssue.builder()
              .category("OPTIONS_GENERATOR")
              .field("optionsGenerator")
              .message(
                  "Options generator is defined but question type is "
                      + templateType
                      + ". It will be ignored.")
              .severity(IssueSeverity.WARNING)
              .build());
    }
  }

  private void validateTags(
      String[] tags, List<ValidationIssue> warnings, List<ValidationIssue> info) {

    if (tags == null || tags.length == 0) {
      warnings.add(
          ValidationIssue.builder()
              .category("TAGS")
              .field("tags")
              .message("No tags defined. Tags help organize and search templates.")
              .severity(IssueSeverity.WARNING)
              .suggestion("Add tags like 'algebra', 'grade-8', 'chapter-1', 'linear-equations'")
              .build());
      return;
    }

    // Check for empty tags
    List<String> emptyTags = new ArrayList<>();
    for (int i = 0; i < tags.length; i++) {
      if (tags[i] == null || tags[i].trim().isEmpty()) {
        emptyTags.add(String.valueOf(i));
      }
    }

    if (!emptyTags.isEmpty()) {
      warnings.add(
          ValidationIssue.builder()
              .category("TAGS")
              .field("tags")
              .message("Empty tags at indices: " + String.join(", ", emptyTags))
              .severity(IssueSeverity.WARNING)
              .build());
    }

    // Info about tag count
    info.add(
        ValidationIssue.builder()
            .category("TAGS")
            .field("tags")
            .message("Template has " + tags.length + " tag(s)")
            .severity(IssueSeverity.INFO)
            .build());
  }

  // ===== Helper Methods =====

  private TemplateValidationResponse buildResponse(
      List<ValidationIssue> errors, List<ValidationIssue> warnings, List<ValidationIssue> info) {

    return TemplateValidationResponse.builder()
        .isValid(errors.isEmpty())
        .errorCount(errors.size())
        .warningCount(warnings.size())
        .errors(errors)
        .warnings(warnings)
        .info(info)
        .build();
  }

  private boolean isJavaScriptKeyword(String word) {
    Set<String> keywords =
        Set.of(
            "var",
            "let",
            "const",
            "function",
            "return",
            "if",
            "else",
            "for",
            "while",
            "true",
            "false",
            "null",
            "undefined",
            "this",
            "new",
            "typeof",
            "instanceof");
    return keywords.contains(word);
  }

  private boolean containsSimilarVariables(String rule1, String rule2) {
    Pattern varPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");

    Set<String> vars1 = new HashSet<>();
    Matcher matcher1 = varPattern.matcher(rule1);
    while (matcher1.find()) {
      vars1.add(matcher1.group(1));
    }

    Set<String> vars2 = new HashSet<>();
    Matcher matcher2 = varPattern.matcher(rule2);
    while (matcher2.find()) {
      vars2.add(matcher2.group(1));
    }

    // Check for any common variables
    vars1.retainAll(vars2);
    return !vars1.isEmpty();
  }

  /**
   * Get JavaScript engine with fallback options for GraalVM
   *
   * @return ScriptEngine or null if not available
   */
  private ScriptEngine getJavaScriptEngine() {
    ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
    if (engine == null) {
      // Try GraalVM engine names
      engine = scriptEngineManager.getEngineByName("graal.js");
      if (engine == null) {
        engine = scriptEngineManager.getEngineByName("Graal.js");
      }
    }
    return engine;
  }
}
