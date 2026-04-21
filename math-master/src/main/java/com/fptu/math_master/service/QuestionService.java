package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {

  /**
   * Create a single question
   */
  QuestionResponse createQuestion(CreateQuestionRequest request);

  /**
   * Get question by ID
   */
  QuestionResponse getQuestionById(UUID id);

  /**
   * Get all questions created by current teacher
   */
  Page<QuestionResponse> getMyQuestions(String name, String tag, Pageable pageable);

  /**
   * Get questions by question bank
   */
  Page<QuestionResponse> getQuestionsByBank(UUID bankId, Pageable pageable);

  /**
   * Get questions by template
   */
  List<QuestionResponse> getQuestionsByTemplate(UUID templateId);

  /**
   * Get questions by canonical question
   */
  Page<QuestionResponse> getQuestionsByCanonicalQuestion(UUID canonicalQuestionId, Pageable pageable);

  /**
   * Update question
   */
  QuestionResponse updateQuestion(UUID id, UpdateQuestionRequest request);

  QuestionResponse approveQuestion(UUID id);

  Integer bulkApproveQuestions(java.util.List<UUID> questionIds);

  /**
   * Delete question (soft delete)
   */
  void deleteQuestion(UUID id);

  /**
   * Import multiple questions from CSV file
   */
  ImportQuestionsResponse importQuestionsFromFile(ImportQuestionsRequest request);

  /**
   * Search questions
   */
  Page<QuestionResponse> searchQuestions(String searchTerm, String type, Pageable pageable);

  /**
   * Assign multiple existing questions into one question bank
   */
  Integer assignQuestionsToBank(UUID bankId, List<UUID> questionIds);

  /**
   * Remove multiple questions from one question bank
   */
  Integer removeQuestionsFromBank(UUID bankId, List<UUID> questionIds);
}
