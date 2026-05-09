package com.fptu.math_master.dto.request;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for Gemini Developer API (generateContent endpoint). POST
 * https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeminiRequest {

  List<Content> contents;
  GenerationConfig generationConfig;
  List<SafetySetting> safetySettings;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Content {
    String role; // "user" | "model"
    List<Part> parts;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Part {
    String text;
    InlineData inlineData;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class InlineData {
    String mimeType;
    String data; // base64 encoded image
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class GenerationConfig {
    Double temperature;
    Integer maxOutputTokens;
    Double topP;
    Integer topK;
    String responseMimeType;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class SafetySetting {
    String category;
    String threshold;
  }
}
