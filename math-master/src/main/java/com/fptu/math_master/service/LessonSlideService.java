package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface LessonSlideService {

  LessonSlideGeneratedContentResponse generateLessonContentDraft(
      LessonSlideGenerateContentRequest request);

  LessonResponse confirmLessonContent(UUID lessonId, LessonSlideConfirmContentRequest request);

  SlideTemplateResponse uploadTemplate(String name, String description, MultipartFile file);

  SlideTemplateResponse updateTemplate(
      UUID templateId,
      String name,
      String description,
      Boolean active,
      MultipartFile file);

  List<SlideTemplateResponse> getTemplates(boolean activeOnly);

  BinaryFileData downloadTemplate(UUID templateId);

  BinaryFileData generatePptx(LessonSlideGeneratePptxRequest request);

  BinaryFileData generatePptxFromJson(LessonSlideGeneratePptxFromJsonRequest request);

  record BinaryFileData(byte[] content, String fileName, String contentType) {}
}
