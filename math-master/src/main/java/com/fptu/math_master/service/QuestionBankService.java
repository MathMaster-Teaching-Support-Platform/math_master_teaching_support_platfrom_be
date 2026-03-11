package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionBankService {

  QuestionBankResponse createQuestionBank(QuestionBankRequest request);

  QuestionBankResponse updateQuestionBank(UUID id, QuestionBankRequest request);

  void deleteQuestionBank(UUID id);

  QuestionBankResponse getQuestionBankById(UUID id);

  Page<QuestionBankResponse> getMyQuestionBanks(Pageable pageable);

  Page<QuestionBankResponse> searchQuestionBanks(
      Boolean isPublic, String searchTerm, Pageable pageable);

  QuestionBankResponse togglePublicStatus(UUID id);

  boolean canEditQuestionBank(UUID id);

  boolean canDeleteQuestionBank(UUID id);
}
