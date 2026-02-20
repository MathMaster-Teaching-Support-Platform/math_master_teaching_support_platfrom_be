package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.OllamaProperties;
import com.fptu.math_master.dto.request.OllamaChatRequest;
import com.fptu.math_master.dto.response.OllamaChatResponse;
import com.fptu.math_master.service.OllamaService;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OllamaServiceImpl implements OllamaService {

  RestClient ollamaRestClient;
  OllamaProperties ollamaProperties;
  ObjectMapper objectMapper = new ObjectMapper();

  public OllamaServiceImpl(
      @Qualifier("ollamaRestClient") RestClient ollamaRestClient,
      OllamaProperties ollamaProperties) {
    this.ollamaRestClient = ollamaRestClient;
    this.ollamaProperties = ollamaProperties;
  }

  @Override
  public OllamaChatResponse sendMessage(String message) {
    log.info("Sending message to Ollama API");

    OllamaChatRequest.Message userMessage =
        OllamaChatRequest.Message.builder().role("user").content(message).build();

    return chat(Collections.singletonList(userMessage));
  }

  @Override
  public OllamaChatResponse chat(List<OllamaChatRequest.Message> messages) {
    log.info("Sending chat request to Ollama with {} messages", messages.size());
    long startTime = System.currentTimeMillis();

    try {
      OllamaChatRequest request =
          OllamaChatRequest.builder().model(ollamaProperties.getModel()).messages(messages).stream(
                  false)
              .build();

      log.info(
          "Calling Ollama API at {} with model {}",
          ollamaProperties.getBaseUrl(),
          ollamaProperties.getModel());

      // Use exchange to get direct access to response body, bypassing message converters
      String raw =
          ollamaRestClient
              .post()
              .uri("/api/chat")
              .body(request)
              .exchange(
                  (req, resp) -> {
                    // Read the raw response body directly
                    java.io.InputStream inputStream = resp.getBody();
                    return new String(
                        inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                  });

      long duration = System.currentTimeMillis() - startTime;
      log.info("Ollama API call completed in {} ms ({} seconds)", duration, duration / 1000.0);

      if (raw == null || raw.isEmpty()) {
        throw new RuntimeException("Empty response from Ollama");
      }

      log.debug("Raw Ollama response: {}", raw);

      // Parse JSON into OllamaChatResponse
      OllamaChatResponse response = objectMapper.readValue(raw, OllamaChatResponse.class);

      log.info("Successfully parsed Ollama response");
      return response;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Error calling Ollama API after {} ms: {}", duration, e.getMessage(), e);
      throw new RuntimeException("Failed to communicate with Ollama: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean testConnection() {
    try {
      log.info("Testing connection to Ollama at {}", ollamaProperties.getBaseUrl());

      // Use exchange to get direct access to response, bypassing message converters
      String response =
          ollamaRestClient
              .get()
              .uri("/api/tags")
              .exchange(
                  (req, resp) -> {
                    java.io.InputStream inputStream = resp.getBody();
                    return new String(
                        inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                  });

      log.info("Successfully connected to Ollama");
      return response != null && !response.isEmpty();

    } catch (Exception e) {
      log.error("Failed to connect to Ollama: {}", e.getMessage());
      return false;
    }
  }
}
