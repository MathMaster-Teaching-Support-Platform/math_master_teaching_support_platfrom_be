package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CanonicalQuestionRequest;
import com.fptu.math_master.dto.response.CanonicalQuestionResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CanonicalQuestionService {

  CanonicalQuestionResponse createCanonicalQuestion(CanonicalQuestionRequest request);

  CanonicalQuestionResponse getCanonicalQuestionById(UUID id);

  Page<CanonicalQuestionResponse> getMyCanonicalQuestions(Pageable pageable);
}