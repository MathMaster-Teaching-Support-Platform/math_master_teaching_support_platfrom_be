package com.fptu.math_master.service;

/** Service for communicating with the Gemini Developer API (Google AI Studio). */
public interface GeminiService {

  /**
   * Send a single-turn text prompt to Gemini and return the generated text.
   *
   * @param prompt The user prompt to send
   * @return The text response from Gemini
   */
  String sendMessage(String prompt);

  /**
   * Test connectivity to the Gemini API.
   *
   * @return true if the API responds successfully
   */
  boolean testConnection();
}
