package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankMatrixStatsResponse;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.dto.response.QuestionBankTreeResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import java.util.List;
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
      String searchTerm, UUID chapterId, Boolean mineOnly, Pageable pageable);

  QuestionBankResponse togglePublicStatus(UUID id);

  QuestionTemplateResponse mapTemplateToBank(UUID bankId, UUID templateId);

  void unmapTemplateFromBank(UUID bankId, UUID templateId);

  List<QuestionTemplateResponse> getMappedTemplates(UUID bankId);

  boolean canEditQuestionBank(UUID id);

  boolean canDeleteQuestionBank(UUID id);

  List<QuestionBankMatrixStatsResponse> getMatrixStats(UUID bankId);

  /**
   * Happy-case tree view: Lớp → Chương → 4 cognitive buckets (NB/TH/VD/VDC).
   * Every chapter of the bank's school grade is returned, even when empty.
   */
  QuestionBankTreeResponse getBankTree(UUID bankId);
}
