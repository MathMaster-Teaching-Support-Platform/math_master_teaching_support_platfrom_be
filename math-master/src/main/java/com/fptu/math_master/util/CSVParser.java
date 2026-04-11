package com.fptu.math_master.util;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CSVParser {

  public static class ParseResult {
    public List<CreateQuestionRequest> questions = new ArrayList<>();
    public List<String> errors = new ArrayList<>();
  }

  public static ParseResult parseCSV(String csvContent) {
    ParseResult result = new ParseResult();

    try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        result.errors.add("CSV file is empty");
        return result;
      }

      String[] headers = parseCSVLine(headerLine);
      Map<String, Integer> headerIndex = buildHeaderIndex(headers);

      int rowNumber = 2; // Start from 2 (header is 1)
      String line;

      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          rowNumber++;
          continue;
        }

        try {
          String[] values = parseCSVLine(line);
          CreateQuestionRequest question = parseRow(values, headerIndex, rowNumber);
          if (question != null) {
            result.questions.add(question);
          }
        } catch (Exception e) {
          result.errors.add("Row " + rowNumber + ": " + e.getMessage());
          log.warn("Error parsing row {}: {}", rowNumber, e.getMessage());
        }

        rowNumber++;
      }
    } catch (Exception e) {
      result.errors.add("Failed to parse CSV: " + e.getMessage());
      log.error("CSV parsing error", e);
    }

    return result;
  }

  private static Map<String, Integer> buildHeaderIndex(String[] headers) {
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      index.put(headers[i].toLowerCase().trim(), i);
    }
    return index;
  }

  private static CreateQuestionRequest parseRow(
      String[] values, Map<String, Integer> headerIndex, int rowNumber) {
    CreateQuestionRequest request = new CreateQuestionRequest();

    // Required: Question Text
    String questionText = getColumnValue(values, headerIndex, "question_text", true);
    if (questionText == null) {
      throw new IllegalArgumentException("Question text is required");
    }
    request.setQuestionText(questionText);

    // Required: Question Type
    String typeStr = getColumnValue(values, headerIndex, "question_type", true);
    if (typeStr == null) {
      throw new IllegalArgumentException("Question type is required");
    }
    try {
      request.setQuestionType(QuestionType.valueOf(typeStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid question type: " + typeStr);
    }

    // Required: Cognitive Level
    String cognitiveStr = getColumnValue(values, headerIndex, "cognitive_level", true);
    if (cognitiveStr == null) {
      throw new IllegalArgumentException("Cognitive level is required");
    }
    try {
      request.setCognitiveLevel(CognitiveLevel.valueOf(cognitiveStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid cognitive level: " + cognitiveStr);
    }

    // Optional fields
    String correctAnswer = getColumnValue(values, headerIndex, "correct_answer", false);
    request.setCorrectAnswer(correctAnswer);

    String explanation = getColumnValue(values, headerIndex, "explanation", false);
    request.setExplanation(explanation);

    String pointsStr = getColumnValue(values, headerIndex, "points", false);
    if (pointsStr != null && !pointsStr.isEmpty()) {
      try {
        request.setPoints(new BigDecimal(pointsStr));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid points value: " + pointsStr);
      }
    } else {
      request.setPoints(BigDecimal.valueOf(1.0));
    }

    String optionsStr = getColumnValue(values, headerIndex, "options", false);
    if (optionsStr != null && !optionsStr.isEmpty()) {
      try {
        Map<String, Object> options = parseOptionsJson(optionsStr);
        request.setOptions(options);
      } catch (Exception e) {
        log.warn("Failed to parse options: {}", optionsStr);
      }
    }

    String tagsStr = getColumnValue(values, headerIndex, "tags", false);
    if (tagsStr != null && !tagsStr.isEmpty()) {
      request.setTags(tagsStr.split(","));
    }

    return request;
  }

  private static String getColumnValue(
      String[] values, Map<String, Integer> headerIndex, String columnName, boolean required) {
    Integer index = headerIndex.get(columnName.toLowerCase());
    if (index == null) {
      if (required) {
        throw new IllegalArgumentException("Required column not found: " + columnName);
      }
      return null;
    }

    if (index >= values.length) {
      return null;
    }

    String value = values[index].trim();
    return value.isEmpty() ? null : value;
  }

  private static String[] parseCSVLine(String line) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          // Escaped quote
          current.append('"');
          i++;
        } else {
          // Toggle quote mode
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        result.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }

    result.add(current.toString());
    return result.toArray(new String[0]);
  }

  private static Map<String, Object> parseOptionsJson(String optionsStr) {
    Map<String, Object> options = new HashMap<>();

    // Simple format: A:Option A, B:Option B, C:Option C
    String[] pairs = optionsStr.split(",");
    for (String pair : pairs) {
      String[] split = pair.trim().split(":", 2);
      if (split.length == 2) {
        options.put(split[0].trim(), split[1].trim());
      }
    }

    return options;
  }
}
