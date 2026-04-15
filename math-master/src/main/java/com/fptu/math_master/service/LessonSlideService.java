package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.response.LessonSlideGeneratedFileResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import com.fptu.math_master.enums.LessonStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface LessonSlideService {

  LessonSlideGeneratedContentResponse generateLessonContentDraft(
      LessonSlideGenerateContentRequest request);

  LessonResponse confirmLessonContent(UUID lessonId, LessonSlideConfirmContentRequest request);

  LessonResponse getLessonSlide(UUID lessonId);

  List<LessonResponse> getLessonSlides(LessonStatus status);

  LessonResponse publishLessonSlide(UUID lessonId);

  LessonResponse unpublishLessonSlide(UUID lessonId);

  LessonResponse getPublishedLessonSlide(UUID lessonId);

  SlideTemplateResponse uploadTemplate(
      String name, String description, MultipartFile file, MultipartFile previewImage);

  SlideTemplateResponse updateTemplate(
      UUID templateId,
      String name,
      String description,
      Boolean active,
      MultipartFile file,
      MultipartFile previewImage);

  List<SlideTemplateResponse> getTemplates(boolean activeOnly);

  BinaryFileData downloadTemplate(UUID templateId);

  BinaryFileData downloadTemplatePreviewImage(UUID templateId);

  BinaryFileData generatePptx(LessonSlideGeneratePptxRequest request);

  BinaryFileData generatePptxFromJson(LessonSlideGeneratePptxFromJsonRequest request);

    List<LessonSlideGeneratedFileResponse> getMyGeneratedSlides(UUID lessonId);

    BinaryFileData downloadGeneratedSlide(UUID generatedFileId);

    String getGeneratedSlidePreviewUrl(UUID generatedFileId);

    LessonSlideGeneratedFileResponse publishGeneratedSlide(UUID generatedFileId);

    LessonSlideGeneratedFileResponse unpublishGeneratedSlide(UUID generatedFileId);

    Page<LessonSlideGeneratedFileResponse> getAllPublicGeneratedSlides(
      UUID lessonId, String keyword, Pageable pageable);

    Page<LessonSlideGeneratedFileResponse> getPublicGeneratedSlidesByLesson(
      UUID lessonId, String keyword, Pageable pageable);

    BinaryFileData downloadPublicGeneratedSlide(UUID generatedFileId);

    String getPublicGeneratedSlidePreviewUrl(UUID generatedFileId);

  record BinaryFileData(byte[] content, String fileName, String contentType) {}
}
