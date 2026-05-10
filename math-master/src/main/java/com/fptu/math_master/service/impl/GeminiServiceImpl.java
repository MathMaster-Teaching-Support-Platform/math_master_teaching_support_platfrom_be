package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.GeminiProperties;
import com.fptu.math_master.dto.request.GeminiRequest;
import com.fptu.math_master.dto.request.GeminiRequest.Content;
import com.fptu.math_master.dto.request.GeminiRequest.Part;
import com.fptu.math_master.dto.request.GeminiRequest.SafetySetting;
import com.fptu.math_master.dto.response.GeminiResponse;
import com.fptu.math_master.service.GeminiService;
import java.util.Arrays;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeminiServiceImpl implements GeminiService {

  private static final int MAX_RETRY_ATTEMPTS = 4;
  private static final long INITIAL_BACKOFF_MS = 800L;

  /**
   * Gemini API output budget per request. TF/MCQ JSON replies with long solutionSteps often exceed
   * 8192; 32k matches {@code sendJsonMessage} historical default and reduces MAX_TOKEN truncation.
   */
  private static final int DEFAULT_MAX_OUTPUT_TOKENS = 32768;

  RestClient geminiRestClient;
  GeminiProperties geminiProperties;
  ObjectMapper objectMapper;

  public GeminiServiceImpl(
      @Qualifier("geminiRestClient") RestClient geminiRestClient,
      GeminiProperties geminiProperties) {
    this.geminiRestClient = geminiRestClient;
    this.geminiProperties = geminiProperties;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public String sendMessage(String prompt) {
    return sendTextPrompt(prompt, DEFAULT_MAX_OUTPUT_TOKENS, null);
  }

  @Override
  public String sendJsonMessage(String prompt) {
    return sendTextPrompt(prompt, DEFAULT_MAX_OUTPUT_TOKENS, "application/json");
  }

  private String sendTextPrompt(String prompt, int maxOutputTokens, String responseMimeType) {
    log.info(
        "Sending message to Gemini API (model: {}, maxOutputTokens: {}, responseMimeType: {})",
        geminiProperties.getModel(),
        maxOutputTokens,
        responseMimeType);
    long startTime = System.currentTimeMillis();

    try {
      GeminiRequest request =
          GeminiRequest.builder()
              .contents(
                  Collections.singletonList(
                      Content.builder()
                          .role("user")
                          .parts(Collections.singletonList(Part.builder().text(prompt).build()))
                          .build()))
              .generationConfig(
                  GeminiRequest.GenerationConfig.builder()
                      .temperature(0.7)
                      .maxOutputTokens(maxOutputTokens)
                      .responseMimeType(responseMimeType)
                      .build())
              .safetySettings(
                  Arrays.asList(
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_HARASSMENT")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_HATE_SPEECH")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_SEXUALLY_EXPLICIT")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_DANGEROUS_CONTENT")
                          .threshold("BLOCK_NONE")
                          .build()))
              .build();

        String raw = callGeminiWithRetry(request, "text-generation");

      long duration = System.currentTimeMillis() - startTime;
      log.info("Gemini API responded in {} ms", duration);

      if (raw == null || raw.isBlank()) {
        throw new RuntimeException("Empty response from Gemini API");
      }

      log.info("Raw Gemini response: {}", raw);

      GeminiResponse response = objectMapper.readValue(raw, GeminiResponse.class);
      String content = response.getTextContent();

      if (content == null || content.isBlank()) {
        log.error("Gemini returned no text content. Full response: {}", raw);
        throw new RuntimeException("Gemini returned no text content");
      }

      return content;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Error calling Gemini API after {} ms: {}", duration, e.getMessage(), e);
      throw new RuntimeException("Failed to communicate with Gemini API: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean testConnection() {
    try {
      log.info("Testing connection to Gemini API");
      String result = sendMessage("Say hello in one word.");
      return result != null && !result.isBlank();
    } catch (Exception e) {
      log.error("Gemini API connection test failed: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public String analyzeImageWithPrompt(byte[] imageBytes, String prompt) {
    log.info("Analyzing image with Gemini API (model: {})", geminiProperties.getModel());
    long startTime = System.currentTimeMillis();

    try {
      // Convert image bytes to base64
      String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

      // Create request with image and text
      GeminiRequest request =
          GeminiRequest.builder()
              .contents(
                  Collections.singletonList(
                      Content.builder()
                          .role("user")
                          .parts(
                              Arrays.asList(
                                  Part.builder().text(prompt).build(),
                                  Part.builder()
                                      .inlineData(
                                          GeminiRequest.InlineData.builder()
                                              .mimeType("image/jpeg")
                                              .data(base64Image)
                                              .build())
                                      .build()))
                          .build()))
              .generationConfig(
                  GeminiRequest.GenerationConfig.builder()
                      .temperature(0.4)
                      .maxOutputTokens(DEFAULT_MAX_OUTPUT_TOKENS)
                      .build())
              .safetySettings(
                  Arrays.asList(
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_HARASSMENT")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_HATE_SPEECH")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_SEXUALLY_EXPLICIT")
                          .threshold("BLOCK_NONE")
                          .build(),
                      SafetySetting.builder()
                          .category("HARM_CATEGORY_DANGEROUS_CONTENT")
                          .threshold("BLOCK_NONE")
                          .build()))
              .build();

        String raw = callGeminiWithRetry(request, "image-analysis");

      long duration = System.currentTimeMillis() - startTime;
      log.info("Gemini API image analysis completed in {} ms", duration);

      if (raw == null || raw.isBlank()) {
        throw new RuntimeException("Empty response from Gemini API");
      }

      GeminiResponse response = objectMapper.readValue(raw, GeminiResponse.class);
      String content = response.getTextContent();

      if (content == null || content.isBlank()) {
        log.error("Gemini returned no text content. Full response: {}", raw);
        throw new RuntimeException("Gemini returned no text content");
      }

      return content;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Error analyzing image with Gemini API after {} ms: {}", duration, e.getMessage(), e);
      throw new RuntimeException("Failed to analyze image with Gemini API: " + e.getMessage(), e);
    }
  }

  private String callGeminiWithRetry(GeminiRequest request, String operationName) throws Exception {
    long backoffMs = INITIAL_BACKOFF_MS;
    GeminiApiException lastTransientException = null;

    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      try {
        String uri =
            "/v1beta/models/"
                + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();

        return geminiRestClient
            .post()
            .uri(uri)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(request)
            .exchange(
                (req, resp) -> {
                  java.io.InputStream inputStream = resp.getBody();
                  String body =
                      new String(
                          inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                  int statusCode = resp.getStatusCode().value();
                  if (statusCode != 200) {
                    throw new GeminiApiException(statusCode, body);
                  }
                  return body;
                });

      } catch (GeminiApiException ex) {
        if (!isTransientStatus(ex.statusCode) || attempt == MAX_RETRY_ATTEMPTS) {
          throw new RuntimeException(
              "Gemini API returned HTTP " + ex.statusCode + ": " + ex.responseBody);
        }

        lastTransientException = ex;
        log.warn(
            "Gemini {} transient failure (HTTP {}) on attempt {}/{}. Retrying in {} ms",
            operationName,
            ex.statusCode,
            attempt,
            MAX_RETRY_ATTEMPTS,
            backoffMs);
        sleepQuietly(backoffMs);
        backoffMs *= 2;
      }
    }

    if (lastTransientException != null) {
      throw new RuntimeException(
          "Gemini API returned HTTP "
              + lastTransientException.statusCode
              + ": "
              + lastTransientException.responseBody);
    }
    throw new RuntimeException("Gemini API call failed");
  }

  private boolean isTransientStatus(int statusCode) {
    return statusCode == 503 || statusCode == 429;
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", ie);
    }
  }

  private static final class GeminiApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    private GeminiApiException(int statusCode, String responseBody) {
      super("Gemini API returned HTTP " + statusCode + ": " + responseBody);
      this.statusCode = statusCode;
      this.responseBody = responseBody;
    }
  }
}
