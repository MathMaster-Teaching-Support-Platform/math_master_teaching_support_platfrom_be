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
   * Send a single-turn text prompt to Gemini in JSON mode (responseMimeType=application/json)
   * with a larger output cap, suitable for prompts that ask the model to return one big JSON
   * object. Use this when the response shape is rich enough that the default 8K token cap risks
   * mid-string truncation.
   *
   * @param prompt The user prompt to send
   * @return The raw JSON text response from Gemini
   */
  String sendJsonMessage(String prompt);

  /**
   * Test connectivity to the Gemini API.
   *
   * @return true if the API responds successfully
   */
  boolean testConnection();

  /**
   * Analyze an image with a text prompt using Gemini Vision API.
   *
   * @param imageBytes The image data as byte array
   * @param prompt The text prompt to guide the analysis
   * @return The text response from Gemini
   */
  String analyzeImageWithPrompt(byte[] imageBytes, String prompt);
}
