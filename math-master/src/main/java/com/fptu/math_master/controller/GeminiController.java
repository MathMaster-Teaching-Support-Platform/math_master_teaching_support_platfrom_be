package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
    name = "AI - Gemini",
    description = "AI endpoints using Gemini Developer API (Google AI Studio)")
public class GeminiController {

  GeminiService geminiService;

  @GetMapping("/test")
  @PermitAll
  @Operation(
      summary = "Test Gemini API connection",
      description = "Send a simple test message to verify the Gemini API is configured correctly.")
  public ApiResponse<String> testConnection() {
    log.info("Testing Gemini API connection");

    boolean isConnected = geminiService.testConnection();

    if (isConnected) {
      return ApiResponse.<String>builder().result("Gemini connection successful!").build();
    } else {
      return ApiResponse.<String>builder()
          .code(500)
          .message("Failed to connect to Gemini API. Please check GEMINI_API_KEY.")
          .build();
    }
  }

  @PostMapping("/chat")
  @Operation(
      summary = "Send message to Gemini",
      description = "Send a text prompt to Gemini and get a response")
  public ApiResponse<String> sendMessage(@RequestBody MessageRequest request) {
    log.info("Received chat request");

    try {
      String response = geminiService.sendMessage(request.getMessage());
      return ApiResponse.<String>builder().result(response).build();
    } catch (Exception e) {
      log.error("Error processing chat request", e);
      return ApiResponse.<String>builder().code(500).message("Error: " + e.getMessage()).build();
    }
  }

  // Inner request class
  @lombok.Data
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class MessageRequest {
    private String message;
  }
}
