package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.QuestionTemplateBatchImportRequest;
import com.fptu.math_master.dto.response.ExcelPreviewResponse;
import com.fptu.math_master.dto.response.TemplateBatchImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportService {

  /**
   * Parse Excel file and return preview with validation results
   *
   * @param file Excel file (.xlsx)
   * @return Preview with valid and invalid rows
   */
  ExcelPreviewResponse previewExcelImport(MultipartFile file);

  /**
   * Import validated templates in batch
   *
   * @param request Batch import request with validated templates
   * @return Import result with success/failure details
   */
  TemplateBatchImportResponse importTemplatesBatch(QuestionTemplateBatchImportRequest request);

  /**
   * Generate Excel template file for download
   *
   * @return Excel file as byte array
   */
  byte[] generateExcelTemplate();
}
