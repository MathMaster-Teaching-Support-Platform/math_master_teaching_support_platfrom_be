package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.OllamaChatRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.OllamaChatResponse;
import com.fptu.math_master.service.OllamaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "AI - Ollama", description = "AI endpoints using Ollama (Llama 3)")
public class OllamaController {

    OllamaService ollamaService;

    @GetMapping("/test")
    @PermitAll
    @Operation(
        summary = "Test Ollama API connection",
        description = "Send a simple test message to verify the Ollama API is configured correctly."
    )
    public ApiResponse<String> testConnection() {
        log.info("Testing Ollama API connection");

        boolean isConnected = ollamaService.testConnection();

        if (isConnected) {
            return ApiResponse.<String>builder()
                    .result("Ollama connection successful!")
                    .build();
        } else {
            return ApiResponse.<String>builder()
                    .code(500)
                    .message("Failed to connect to Ollama. Please ensure Ollama is running.")
                    .build();
        }
    }

    @PostMapping("/chat")
    @Operation(
        summary = "Send message to Ollama",
        description = "Send a message to Ollama and get a response"
    )
    public ApiResponse<OllamaChatResponse> sendMessage(@RequestBody MessageRequest request) {
        log.info("Received chat request: {}", request.getMessage());

        try {
            OllamaChatResponse response = ollamaService.sendMessage(request.getMessage());

            return ApiResponse.<OllamaChatResponse>builder()
                    .result(response)
                    .build();

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ApiResponse.<OllamaChatResponse>builder()
                    .code(500)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/chat/conversation")
    @Operation(
        summary = "Send conversation to Ollama",
        description = "Send a conversation history to Ollama and get a response"
    )
    public ApiResponse<OllamaChatResponse> chatWithHistory(
            @RequestBody ConversationRequest request) {
        log.info("Received conversation request with {} messages", request.getMessages().size());

        try {
            OllamaChatResponse response = ollamaService.chat(request.getMessages());

            return ApiResponse.<OllamaChatResponse>builder()
                    .result(response)
                    .build();

        } catch (Exception e) {
            log.error("Error processing conversation request", e);
            return ApiResponse.<OllamaChatResponse>builder()
                    .code(500)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    // Inner request classes
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageRequest {
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationRequest {
        private List<OllamaChatRequest.Message> messages;
    }
}
