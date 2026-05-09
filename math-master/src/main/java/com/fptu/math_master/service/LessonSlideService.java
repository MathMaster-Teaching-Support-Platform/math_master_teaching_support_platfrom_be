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

  SlideTemplateResponse getTemplateById(UUID templateId);

  void deleteTemplate(UUID templateId);

  BinaryFileData downloadTemplate(UUID templateId);

  BinaryFileData downloadTemplatePreviewImage(UUID templateId);

  SlideTemplateResponse regenerateTemplatePreview(UUID templateId);

  BinaryFileData generatePptx(LessonSlideGeneratePptxRequest request);

  BinaryFileData generatePptxFromJson(LessonSlideGeneratePptxFromJsonRequest request);

    List<LessonSlideGeneratedFileResponse> getMyGeneratedSlides(
      UUID gradeId, UUID subjectId, UUID chapterId, UUID lessonId, String keyword);

    BinaryFileData downloadGeneratedSlide(UUID generatedFileId);

    BinaryFileData getGeneratedSlidePreviewPdf(UUID generatedFileId);

    String getGeneratedSlidePreviewUrl(UUID generatedFileId);

    LessonSlideGeneratedFileResponse publishGeneratedSlide(UUID generatedFileId);

    LessonSlideGeneratedFileResponse unpublishGeneratedSlide(UUID generatedFileId);

    LessonSlideGeneratedFileResponse updateGeneratedSlideMetadata(
      UUID generatedFileId, String name, MultipartFile thumbnailFile);

    BinaryFileData getGeneratedSlideThumbnailImage(UUID generatedFileId);

    Page<LessonSlideGeneratedFileResponse> getAllPublicGeneratedSlides(
      UUID gradeId,
      UUID subjectId,
      UUID chapterId,
      UUID lessonId,
      String keyword,
      Pageable pageable);

    Page<LessonSlideGeneratedFileResponse> getPublicGeneratedSlidesByLesson(
      UUID lessonId,
      UUID gradeId,
      UUID subjectId,
      UUID chapterId,
      String keyword,
      Pageable pageable);

    BinaryFileData downloadPublicGeneratedSlide(UUID generatedFileId);

    void deleteGeneratedSlide(UUID generatedFileId);

    BinaryFileData getPublicGeneratedSlidePreviewPdf(UUID generatedFileId);

    String getPublicGeneratedSlidePreviewUrl(UUID generatedFileId);

    BinaryFileData getPublicGeneratedSlideThumbnailImage(UUID generatedFileId);

  /** Re-renders a single slide's heading+content via QuickLaTeX and returns the image URL.
   *  Returns null if the output format is not LATEX or if rendering fails. */
  String renderSlidePreview(String heading, String content);

  record BinaryFileData(byte[] content, String fileName, String contentType) {}
}
