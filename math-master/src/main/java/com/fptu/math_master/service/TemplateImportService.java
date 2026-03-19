package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.TemplateImportResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Service for importing question templates from files with AI assistance */
public interface TemplateImportService {

  /**
   * Import and analyze question template from file
   *
   * @param file Uploaded file (Word, PDF, or Text)
   * @param subjectHint Optional subject/topic hint
   * @param contextHint Optional context hint
   * @param questionBankId Optional question bank ID to assign the imported template to
   * @return Analysis and suggested template draft
   */
  TemplateImportResponse importTemplateFromFile(
      MultipartFile file, String subjectHint, String contextHint, UUID questionBankId);

  /**
   * Extract text content from uploaded file
   *
   * @param file Uploaded file
   * @return Extracted text content
   */
  String extractTextFromFile(MultipartFile file);

  /**
   * Validate uploaded file
   *
   * @param file Uploaded file
   * @return true if valid, false otherwise
   */
  boolean validateFile(MultipartFile file);
}
