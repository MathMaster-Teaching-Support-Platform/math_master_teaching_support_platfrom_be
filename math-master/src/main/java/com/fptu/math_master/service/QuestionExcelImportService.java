package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionBatchImportRequest;
import com.fptu.math_master.dto.response.QuestionBatchImportResponse;
import com.fptu.math_master.dto.response.QuestionExcelPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionExcelImportService {

  QuestionExcelPreviewResponse previewExcelImport(MultipartFile file);

  QuestionBatchImportResponse importQuestionsBatch(QuestionBatchImportRequest request);

  byte[] generateExcelTemplate();
}
