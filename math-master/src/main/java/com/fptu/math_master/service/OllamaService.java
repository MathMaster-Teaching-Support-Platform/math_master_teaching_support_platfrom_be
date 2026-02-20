package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.OllamaChatRequest;
import com.fptu.math_master.dto.response.OllamaChatResponse;
import java.util.List;

public interface OllamaService {

  /**
   * Send a message to Ollama and get a response
   *
   * @param message The message to send
   * @return The response from Ollama
   */
  OllamaChatResponse sendMessage(String message);

  /**
   * Send a chat request with conversation history
   *
   * @param messages List of conversation messages
   * @return The response from Ollama
   */
  OllamaChatResponse chat(List<OllamaChatRequest.Message> messages);

  /**
   * Test connection to Ollama service
   *
   * @return True if connection is successful
   */
  boolean testConnection();
}
