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
    log.info("Sending message to Gemini API (model: {})", geminiProperties.getModel());
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
                      .maxOutputTokens(8192)
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

      String uri =
          "/v1beta/models/"
              + geminiProperties.getModel()
              + ":generateContent?key="
              + geminiProperties.getApiKey();

      String raw =
          geminiRestClient
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
                      throw new RuntimeException(
                          "Gemini API returned HTTP " + statusCode + ": " + body);
                    }
                    return body;
                  });

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
}
