package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Response DTO for Gemini Developer API (generateContent endpoint).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

  List<Candidate> candidates;

  @JsonProperty("usageMetadata")
  UsageMetadata usageMetadata;

  @JsonProperty("modelVersion")
  String modelVersion;

  @JsonProperty("promptFeedback")
  PromptFeedback promptFeedback;

  /**
   * Convenience method to extract the text content from the first candidate.
   */
  public String getTextContent() {
    // Check for prompt-level block
    if (promptFeedback != null && promptFeedback.getBlockReason() != null) {
      log.warn("Gemini blocked prompt: {}", promptFeedback.getBlockReason());
      throw new RuntimeException("Gemini blocked request: " + promptFeedback.getBlockReason());
    }

    if (candidates == null || candidates.isEmpty()) {
      log.warn("Gemini response has no candidates");
      return null;
    }

    Candidate first = candidates.get(0);

    // Check finish reason for safety/error
    if (first.getFinishReason() != null) {
      String reason = first.getFinishReason();
      if ("SAFETY".equals(reason) || "RECITATION".equals(reason) || "BLOCKLIST".equals(reason)) {
        log.warn("Gemini candidate blocked, finishReason: {}", reason);
        throw new RuntimeException("Gemini blocked candidate: " + reason);
      }
    }

    if (first.getContent() == null) {
      log.warn("Gemini candidate has no content, finishReason: {}", first.getFinishReason());
      return null;
    }
    if (first.getContent().getParts() == null || first.getContent().getParts().isEmpty()) {
      log.warn("Gemini candidate content has no parts");
      return null;
    }
    return first.getContent().getParts().get(0).getText();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Candidate {
    Content content;
    String finishReason;
    Integer index;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Content {
    List<Part> parts;
    String role;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Part {
    String text;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UsageMetadata {
    @JsonProperty("promptTokenCount")
    Integer promptTokenCount;

    @JsonProperty("candidatesTokenCount")
    Integer candidatesTokenCount;

    @JsonProperty("totalTokenCount")
    Integer totalTokenCount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PromptFeedback {
    @JsonProperty("blockReason")
    String blockReason;
  }
}

