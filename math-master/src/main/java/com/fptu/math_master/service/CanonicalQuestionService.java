package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CanonicalQuestionRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.response.CanonicalQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CanonicalQuestionService {

  CanonicalQuestionResponse createCanonicalQuestion(CanonicalQuestionRequest request);

  CanonicalQuestionResponse updateCanonicalQuestion(UUID id, CanonicalQuestionRequest request);

  void deleteCanonicalQuestion(UUID id);

  CanonicalQuestionResponse getCanonicalQuestionById(UUID id);

  Page<QuestionResponse> getQuestionsByCanonicalQuestion(UUID id, Pageable pageable);

  Page<CanonicalQuestionResponse> getMyCanonicalQuestions(Pageable pageable);

  GeneratedQuestionsBatchResponse generateQuestionsFromCanonical(
      UUID canonicalQuestionId, GenerateCanonicalQuestionsRequest request);
}