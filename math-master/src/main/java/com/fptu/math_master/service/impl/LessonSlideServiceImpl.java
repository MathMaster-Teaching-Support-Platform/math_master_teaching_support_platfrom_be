package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.request.LessonSlideJsonItemRequest;
import com.fptu.math_master.dto.response.LessonSlideGeneratedFileResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.LessonSlideJsonItemResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import com.fptu.math_master.enums.LessonSlideOutputFormat;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.LessonSlideGeneratedFile;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.SlideTemplate;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.LessonSlideGeneratedFileRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SlideTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.LatexRenderService;
import com.fptu.math_master.service.LessonSlideService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.awt.Dimension;
import java.time.Instant;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonSlideServiceImpl implements LessonSlideService {

  private static final Pattern NON_ALNUM = Pattern.compile("[^a-zA-Z0-9._-]");
  private static final Pattern LATEX_SEGMENT_PATTERN =
      Pattern.compile(
          "(?s)\\\\\\((.+?)\\\\\\)|\\\\\\[(.+?)\\\\\\]|\\$\\$(.+?)\\$\\$|(?<!\\\\)\\$(.+?)(?<!\\\\)\\$");
  private static final float LATEX_FONT_SIZE = 22f;
  private static final double LATEX_HORIZONTAL_PADDING = 2d;
  private static final double LATEX_VERTICAL_PADDING = 6d;
  private static final double LATEX_MAX_IMAGE_HEIGHT = 120d;
  private static final double LATEX_MIN_IMAGE_HEIGHT = 20d;
  private static final double LATEX_ESTIMATED_CHAR_WIDTH = 7d;
  private static final double LATEX_ESTIMATED_LINE_HEIGHT = 22d;
  private static final String LATEX_INLINE_PLACEHOLDER = "            ";
  private static final List<String> TAGGED_SECTIONS =
      List.of(
          "LESSON_SUMMARY",
          "LEARNING_OBJECTIVES",
          "OPENING",
          "MAIN_PART_1",
          "MAIN_PART_2",
          "MAIN_PART_3",
          "EXAMPLE_PART",
          "PRACTICE_PART",
          "CLOSING_SUMMARY",
          "ADDITIONAL_NOTES");
  private static final String PPTX_MIME =
      "application/vnd.openxmlformats-officedocument.presentationml.presentation";
  private static final String PNG_MIME = "image/png";
  private static final String JPG_MIME = "image/jpeg";
  private static final String WEBP_MIME = "image/webp";

  private static final String QUICKLATEX_PREAMBLE =
      "\\usepackage[utf8]{inputenc}\n"
          + "\\usepackage[T5]{fontenc}\n"
          + "\\usepackage{lmodern}\n"
          + "\\usepackage{tikz}\n"
          + "\\usepackage{pgfplots}\n"
          + "\\usepackage{tkz-euclide}\n"
          + "\\usepackage{tkz-tab}\n"
          + "\\usepackage{amsmath}\n"
          + "\\usepackage{amssymb}\n"
          + "\\pgfplotsset{compat=newest}";
  private static final HttpClient IMAGE_HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;
  SlideTemplateRepository slideTemplateRepository;
  LessonSlideGeneratedFileRepository lessonSlideGeneratedFileRepository;
  MinioClient minioClient;
  MinioProperties minioProperties;
  GeminiService geminiService;
  LatexRenderService latexRenderService;
  UserSubscriptionService userSubscriptionService;
  UploadService uploadService;
  ObjectMapper objectMapper;

  @Override
  @Transactional
  public LessonSlideGeneratedContentResponse generateLessonContentDraft(
      LessonSlideGenerateContentRequest request) {
    validateTeacherRole();
    userSubscriptionService.consumeMyTokens(3, "SLIDE");

    SchoolGrade requestedGrade = resolveRequestedGrade(request);

    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    if (Boolean.FALSE.equals(subject.getIsActive()) || subject.getDeletedAt() != null) {
      throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
    }

    if (subject.getSchoolGrade() == null
        || !subject.getSchoolGrade().getId().equals(requestedGrade.getId())
        || Boolean.FALSE.equals(subject.getSchoolGrade().getIsActive())
        || subject.getSchoolGrade().getDeletedAt() != null) {
      throw new AppException(ErrorCode.INVALID_SUBJECT);
    }

    Chapter chapter =
        chapterRepository
            .findById(request.getChapterId())
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    if (chapter.getSubjectId() == null || !chapter.getSubjectId().equals(subject.getId())) {
      throw new AppException(ErrorCode.INVALID_SUBJECT);
    }

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    if (!lesson.getChapterId().equals(chapter.getId())) {
      throw new AppException(ErrorCode.CHAPTER_NOT_IN_LESSON);
    }

    int slideCount = normalizeRequestedSlideCount(request.getSlideCount());
    LessonSlideOutputFormat outputFormat = resolveOutputFormat(request.getOutputFormat());
    DeckSections deckSections =
        buildDeckSectionsWithAi(lesson, request.getAdditionalPrompt(), outputFormat, slideCount);
    List<LessonSlideJsonItemResponse> slides = buildPreviewSlides(deckSections, slideCount, outputFormat);

    return LessonSlideGeneratedContentResponse.builder()
        .subjectId(subject.getId())
        .chapterId(chapter.getId())
        .lessonId(lesson.getId())
        .lessonTitle(lesson.getTitle())
        .slideCount(slideCount)
        .outputFormat(outputFormat)
        .slides(slides)
        .additionalPrompt(request.getAdditionalPrompt())
        .build();
  }

  private int normalizeRequestedSlideCount(Integer requestedSlideCount) {
    if (requestedSlideCount == null) {
      return 10;
    }
    return Math.max(5, Math.min(15, requestedSlideCount));
  }

  private SchoolGrade resolveRequestedGrade(LessonSlideGenerateContentRequest request) {
    SchoolGrade grade =
        schoolGradeRepository
            .findByIdAndNotDeleted(request.getSchoolGradeId())
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
    if (Boolean.FALSE.equals(grade.getIsActive())) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND);
    }
    return grade;
  }

  @Override
  @Transactional
  public LessonResponse confirmLessonContent(
      UUID lessonId, LessonSlideConfirmContentRequest request) {
    validateTeacherRole();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    lesson.setLessonContent(request.getLessonContent());
    if (request.getSummary() != null && !request.getSummary().isBlank()) {
      lesson.setSummary(request.getSummary());
    } else {
      lesson.setSummary(buildSummary(request.getLessonContent()));
    }
    if (request.getLearningObjectives() != null && !request.getLearningObjectives().isBlank()) {
      lesson.setLearningObjectives(request.getLearningObjectives());
    }
    if (lesson.getCreatedBy() == null) {
      lesson.setCreatedBy(currentUserId);
    }
    lesson.setUpdatedBy(currentUserId);

    Lesson saved = lessonRepository.save(lesson);
    return toLessonResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public LessonResponse getLessonSlide(UUID lessonId) {
    validateTeacherRole();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    return toLessonResponse(lesson);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonResponse> getLessonSlides(LessonStatus status) {
    validateTeacherRole();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    LessonStatus targetStatus = status == null ? LessonStatus.DRAFT : status;
    return lessonRepository.findTeacherSlideLessonsByStatus(currentUserId, targetStatus).stream()
        .map(this::toLessonResponse)
        .toList();
  }

  @Override
  @Transactional
  public LessonResponse publishLessonSlide(UUID lessonId) {
    validateTeacherRole();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    if (lesson.getCreatedBy() == null) {
      lesson.setCreatedBy(currentUserId);
    }
    lesson.setUpdatedBy(currentUserId);
    lesson.setStatus(LessonStatus.PUBLISHED);
    return toLessonResponse(lessonRepository.save(lesson));
  }

  @Override
  @Transactional
  public LessonResponse unpublishLessonSlide(UUID lessonId) {
    validateTeacherRole();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    if (lesson.getCreatedBy() == null) {
      lesson.setCreatedBy(currentUserId);
    }
    lesson.setUpdatedBy(currentUserId);
    lesson.setStatus(LessonStatus.DRAFT);
    return toLessonResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse getPublishedLessonSlide(UUID lessonId) {
    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    // Only return published lessons
    if (lesson.getStatus() != LessonStatus.PUBLISHED) {
      throw new AppException(ErrorCode.LESSON_NOT_FOUND); // Use existing error code
    }

    return toLessonResponse(lesson);
  }

  @Override
  @Transactional
  public SlideTemplateResponse uploadTemplate(
      String name, String description, MultipartFile file, MultipartFile previewImage) {
    validateTeacherRole();
    validatePptxFile(file);
    validatePreviewImage(previewImage);
    ensureTemplateBucketExists();

    UUID userId = SecurityUtils.getCurrentUserId();
    String objectKey = buildObjectKey(file.getOriginalFilename());
    String previewImageObjectKey = null;
    String previewImageContentType = null;

    try {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(minioProperties.getTemplateBucket())
              .object(objectKey)
              .contentType(file.getContentType() == null ? PPTX_MIME : file.getContentType())
              .stream(file.getInputStream(), file.getSize(), -1)
              .build());

      if (previewImage != null && !previewImage.isEmpty()) {
        previewImageObjectKey = buildPreviewImageObjectKey(previewImage.getOriginalFilename());
        previewImageContentType = resolvePreviewImageContentType(previewImage);
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getTemplateBucket())
                .object(previewImageObjectKey)
                .contentType(previewImageContentType)
                .stream(previewImage.getInputStream(), previewImage.getSize(), -1)
                .build());
      }
    } catch (Exception ex) {
      log.error("Failed to upload template to minio", ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }

    SlideTemplate template =
        SlideTemplate.builder()
            .name(name)
            .description(description)
            .originalFileName(file.getOriginalFilename())
            .contentType(file.getContentType() == null ? PPTX_MIME : file.getContentType())
            .objectKey(objectKey)
            .previewImageObjectKey(previewImageObjectKey)
            .previewImageContentType(previewImageContentType)
            .bucketName(minioProperties.getTemplateBucket())
            .uploadedBy(userId)
            .isActive(true)
            .build();

    return toTemplateResponse(slideTemplateRepository.save(template));
  }

  @Override
  @Transactional
  public SlideTemplateResponse updateTemplate(
      UUID templateId,
      String name,
      String description,
      Boolean active,
      MultipartFile file,
      MultipartFile previewImage) {
    validateTeacherRole();
    validatePreviewImage(previewImage);

    SlideTemplate template =
        slideTemplateRepository
            .findByIdAndNotDeleted(templateId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (name != null && !name.isBlank()) {
      template.setName(name);
    }
    if (description != null) {
      template.setDescription(description);
    }
    if (active != null) {
      template.setIsActive(active);
    }

    if (file != null && !file.isEmpty()) {
      validatePptxFile(file);
      ensureTemplateBucketExists();
      String objectKey = buildObjectKey(file.getOriginalFilename());
      try {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getTemplateBucket())
                .object(objectKey)
                .contentType(file.getContentType() == null ? PPTX_MIME : file.getContentType())
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());
      } catch (Exception ex) {
        log.error("Failed to update template file in minio", ex);
        throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
      }

      template.setObjectKey(objectKey);
      template.setBucketName(minioProperties.getTemplateBucket());
      template.setOriginalFileName(file.getOriginalFilename());
      template.setContentType(file.getContentType() == null ? PPTX_MIME : file.getContentType());
    }

    if (previewImage != null && !previewImage.isEmpty()) {
      String previewObjectKey = buildPreviewImageObjectKey(previewImage.getOriginalFilename());
      String previewContentType = resolvePreviewImageContentType(previewImage);
      try {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getTemplateBucket())
                .object(previewObjectKey)
                .contentType(previewContentType)
                .stream(previewImage.getInputStream(), previewImage.getSize(), -1)
                .build());
      } catch (Exception ex) {
        log.error("Failed to update template preview image in minio", ex);
        throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
      }

      template.setPreviewImageObjectKey(previewObjectKey);
      template.setPreviewImageContentType(previewContentType);
    }

    return toTemplateResponse(slideTemplateRepository.save(template));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SlideTemplateResponse> getTemplates(boolean activeOnly) {
    List<SlideTemplate> templates =
        activeOnly
            ? slideTemplateRepository.findAllActiveNotDeleted()
            : slideTemplateRepository.findAllNotDeleted();
    return templates.stream().map(this::toTemplateResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData downloadTemplate(UUID templateId) {
    validateTeacherRole();

    SlideTemplate template =
        slideTemplateRepository
            .findByIdAndNotDeleted(templateId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    byte[] content = readObject(template.getBucketName(), template.getObjectKey());
    return new BinaryFileData(content, template.getOriginalFileName(), template.getContentType());
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData downloadTemplatePreviewImage(UUID templateId) {
    validateTeacherRole();

    SlideTemplate template =
        slideTemplateRepository
            .findByIdAndNotDeleted(templateId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getPreviewImageObjectKey() == null || template.getPreviewImageObjectKey().isBlank()) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    byte[] content = readObject(template.getBucketName(), template.getPreviewImageObjectKey());
    String contentType =
        (template.getPreviewImageContentType() == null || template.getPreviewImageContentType().isBlank())
            ? PNG_MIME
            : template.getPreviewImageContentType();
    return new BinaryFileData(content, "template-preview-image", contentType);
  }

  @Override
  @Transactional
  public BinaryFileData generatePptx(LessonSlideGeneratePptxRequest request) {
    validateTeacherRole();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    SlideTemplate template =
        slideTemplateRepository
            .findByIdAndNotDeleted(request.getTemplateId())
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (Boolean.FALSE.equals(template.getIsActive())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    byte[] templateBytes = readObject(template.getBucketName(), template.getObjectKey());

    DeckSections deckSections = buildDeckSections(lesson, request.getAdditionalPrompt());
    byte[] generated = injectTemplate(templateBytes, deckSections);
    String outputName = buildOutputFileName(lesson.getTitle());
    persistGeneratedSlideFile(lesson, template, generated, outputName);
    return new BinaryFileData(generated, outputName, PPTX_MIME);
  }

  @Override
  @Transactional
  public BinaryFileData generatePptxFromJson(LessonSlideGeneratePptxFromJsonRequest request) {
    validateTeacherRole();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    SlideTemplate template =
        slideTemplateRepository
            .findByIdAndNotDeleted(request.getTemplateId())
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (Boolean.FALSE.equals(template.getIsActive())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    List<LessonSlideJsonItemRequest> sortedSlides =
        request.getSlides().stream()
            .sorted(Comparator.comparing(LessonSlideJsonItemRequest::getSlideNumber))
            .toList();

    byte[] templateBytes = readObject(template.getBucketName(), template.getObjectKey());
    LessonSlideOutputFormat outputFormat =
        request.getOutputFormat() != null
            ? request.getOutputFormat()
            : LessonSlideOutputFormat.PLAIN_TEXT;
    byte[] generated = injectTemplateWithJson(templateBytes, lesson, sortedSlides, outputFormat);
    String outputName = buildOutputFileName(lesson.getTitle());
    persistGeneratedSlideFile(lesson, template, generated, outputName);
    return new BinaryFileData(generated, outputName, PPTX_MIME);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonSlideGeneratedFileResponse> getMyGeneratedSlides(UUID lessonId) {
    validateTeacherRole();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    List<LessonSlideGeneratedFile> files =
        lessonId == null
            ? lessonSlideGeneratedFileRepository.findByTeacher(currentUserId)
            : lessonSlideGeneratedFileRepository.findByTeacherAndLesson(currentUserId, lessonId);

    return files.stream().map(this::toGeneratedFileResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData downloadGeneratedSlide(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getObjectKey());
    return new BinaryFileData(content, generatedFile.getFileName(), generatedFile.getContentType());
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData getGeneratedSlidePreviewPdf(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getObjectKey());
    return convertPptxToPdf(content, generatedFile.getFileName());
  }

  @Override
  @Transactional(readOnly = true)
  public String getGeneratedSlidePreviewUrl(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    return uploadService.getPresignedUrl(generatedFile.getObjectKey(), generatedFile.getBucketName());
  }

  @Override
  @Transactional
  public LessonSlideGeneratedFileResponse publishGeneratedSlide(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    generatedFile.setIsPublic(Boolean.TRUE);
    generatedFile.setPublishedAt(Instant.now());
    return toGeneratedFileResponse(lessonSlideGeneratedFileRepository.save(generatedFile));
  }

  @Override
  @Transactional
  public LessonSlideGeneratedFileResponse unpublishGeneratedSlide(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    generatedFile.setIsPublic(Boolean.FALSE);
    generatedFile.setPublishedAt(null);
    return toGeneratedFileResponse(lessonSlideGeneratedFileRepository.save(generatedFile));
  }

  @Override
  @Transactional
  public void deleteGeneratedSlide(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);

    // Soft-delete the DB record.
    generatedFile.setDeletedAt(Instant.now());
    lessonSlideGeneratedFileRepository.save(generatedFile);

    // Best-effort deletion of the actual object from MinIO.
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(generatedFile.getBucketName())
              .object(generatedFile.getObjectKey())
              .build());
    } catch (Exception ex) {
      log.warn(
          "Failed to remove MinIO object during slide deletion. bucket={}, key={}: {}",
          generatedFile.getBucketName(),
          generatedFile.getObjectKey(),
          ex.getMessage());
    }
  }

  @Override
  @Transactional
  public LessonSlideGeneratedFileResponse updateGeneratedSlideMetadata(
      UUID generatedFileId, String name, MultipartFile thumbnailFile) {
    validateTeacherRole();

    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);

    if (name != null) {
      String normalizedName = name.trim();
      generatedFile.setName(normalizedName.isEmpty() ? null : normalizedName);
    }
    if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
      validatePreviewImage(thumbnailFile);
      ensureTemplateBucketExists();

      String thumbnailObjectKey = buildGeneratedSlideThumbnailObjectKey(thumbnailFile.getOriginalFilename());
      String thumbnailContentType = resolvePreviewImageContentType(thumbnailFile);

      try {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getTemplateBucket())
                .object(thumbnailObjectKey)
                .contentType(thumbnailContentType)
                .stream(thumbnailFile.getInputStream(), thumbnailFile.getSize(), -1)
                .build());
      } catch (Exception ex) {
        log.error("Failed to upload generated slide thumbnail", ex);
        throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
      }

      generatedFile.setThumbnail(thumbnailObjectKey);
    }

    return toGeneratedFileResponse(lessonSlideGeneratedFileRepository.save(generatedFile));
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData getGeneratedSlideThumbnailImage(UUID generatedFileId) {
    validateTeacherRole();
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findByIdAndNotDeleted(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    validateGeneratedFileOwnerOrAdmin(generatedFile);
    if (generatedFile.getThumbnail() == null || generatedFile.getThumbnail().isBlank()) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getThumbnail());
    return new BinaryFileData(
        content,
        "generated-slide-thumbnail",
        resolveImageContentTypeFromObjectKey(generatedFile.getThumbnail()));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LessonSlideGeneratedFileResponse> getAllPublicGeneratedSlides(
      UUID lessonId, String keyword, Pageable pageable) {
    String normalizedKeyword = keyword == null ? null : keyword.trim();
    return lessonSlideGeneratedFileRepository
        .findAllPublicWithFilters(lessonId, normalizedKeyword, pageable)
        .map(this::toGeneratedFileResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LessonSlideGeneratedFileResponse> getPublicGeneratedSlidesByLesson(
      UUID lessonId, String keyword, Pageable pageable) {
    lessonRepository
        .findByIdAndNotDeleted(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    String normalizedKeyword = keyword == null ? null : keyword.trim();
    return lessonSlideGeneratedFileRepository
        .findAllPublicWithFilters(lessonId, normalizedKeyword, pageable)
        .map(this::toGeneratedFileResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData downloadPublicGeneratedSlide(UUID generatedFileId) {
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findPublicById(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    lessonRepository
        .findByIdAndNotDeleted(generatedFile.getLessonId())
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getObjectKey());
    return new BinaryFileData(content, generatedFile.getFileName(), generatedFile.getContentType());
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData getPublicGeneratedSlidePreviewPdf(UUID generatedFileId) {
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findPublicById(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    lessonRepository
        .findByIdAndNotDeleted(generatedFile.getLessonId())
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getObjectKey());
    return convertPptxToPdf(content, generatedFile.getFileName());
  }

  @Override
  @Transactional(readOnly = true)
  public String getPublicGeneratedSlidePreviewUrl(UUID generatedFileId) {
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findPublicById(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    lessonRepository
        .findByIdAndNotDeleted(generatedFile.getLessonId())
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    return uploadService.getPresignedUrl(generatedFile.getObjectKey(), generatedFile.getBucketName());
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData getPublicGeneratedSlideThumbnailImage(UUID generatedFileId) {
    LessonSlideGeneratedFile generatedFile =
        lessonSlideGeneratedFileRepository
            .findPublicById(generatedFileId)
            .orElseThrow(() -> new AppException(ErrorCode.GENERATED_SLIDE_NOT_FOUND));

    lessonRepository
        .findByIdAndNotDeleted(generatedFile.getLessonId())
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    if (generatedFile.getThumbnail() == null || generatedFile.getThumbnail().isBlank()) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    byte[] content = readObject(generatedFile.getBucketName(), generatedFile.getThumbnail());
    return new BinaryFileData(
        content,
        "generated-slide-thumbnail",
        resolveImageContentTypeFromObjectKey(generatedFile.getThumbnail()));
  }

  private byte[] injectTemplate(byte[] templateBytes, DeckSections deckSections) {
    try (XMLSlideShow slideshow = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      for (int i = 0; i < slideshow.getSlides().size(); i++) {
        XSLFSlide slide = slideshow.getSlides().get(i);
        Map<String, String> slideValues = buildSlidePlaceholders(i + 1, deckSections);
        List<XSLFShape> shapesSnapshot = new ArrayList<>(slide.getShapes());
        for (XSLFShape shape : shapesSnapshot) {
          if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text == null || text.isBlank()) {
              continue;
            }
            String replaced = replacePlaceholders(text, slideValues);
            if (!text.equals(replaced)) {
              applyTextAndLatex(slideshow, slide, textShape, replaced);
            }
          }
        }
      }

      slideshow.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception ex) {
      log.error("Failed to generate pptx from template", ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private String replacePlaceholders(String text, Map<String, String> values) {
    String replaced = text;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      replaced = replaced.replace(entry.getKey(), entry.getValue());
    }
    return normalizeEscapedLineBreaks(replaced);
  }

  /** Replaces placeholder tags in every text shape of a slide. */
  private void replacePlaceholdersInSlide(
      XMLSlideShow slideshow, XSLFSlide slide, Map<String, String> values) {
    replacePlaceholdersInSlideExcept(slideshow, slide, values, null);
  }

  /**
   * Replaces placeholder tags in every text shape of a slide, skipping {@code excludeShape}.
   * Pass {@code null} for {@code excludeShape} to process all shapes.
   */
  private void replacePlaceholdersInSlideExcept(
      XMLSlideShow slideshow,
      XSLFSlide slide,
      Map<String, String> values,
      XSLFTextShape excludeShape) {
    List<XSLFShape> shapesSnapshot = new ArrayList<>(slide.getShapes());
    for (XSLFShape shape : shapesSnapshot) {
      if (shape instanceof XSLFTextShape textShape) {
        if (textShape == excludeShape) continue;
        String text = textShape.getText();
        if (text == null || text.isBlank()) continue;
        String replaced = replacePlaceholders(text, values);
        if (!text.equals(replaced)) {
          applyTextAndLatex(slideshow, slide, textShape, replaced);
        }
      }
    }
  }

  private BinaryFileData convertPptxToPdf(byte[] pptxContent, String fileName) {
    final float renderScale = 2.0f;
    try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(pptxContent));
        PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      Dimension pageSize = slideShow.getPageSize();
      float slideWidth = (float) pageSize.getWidth();
      float slideHeight = (float) pageSize.getHeight();
      int imageWidth = Math.max(1, Math.round(slideWidth * renderScale));
      int imageHeight = Math.max(1, Math.round(slideHeight * renderScale));

      for (XSLFSlide slide : slideShow.getSlides()) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fill(new Rectangle2D.Float(0, 0, imageWidth, imageHeight));
        graphics.scale(renderScale, renderScale);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        slide.draw(graphics);
        graphics.dispose();

        PDPage page = new PDPage(new PDRectangle(slideWidth, slideHeight));
        document.addPage(page);
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          contentStream.drawImage(pdImage, 0, 0, slideWidth, slideHeight);
        }
      }

      document.save(outputStream);
      return new BinaryFileData(outputStream.toByteArray(), toPdfFileName(fileName), "application/pdf");
    } catch (Exception ex) {
      log.error("Failed to convert generated slide to PDF: {}", fileName, ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private String toPdfFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "slide-preview.pdf";
    }
    int dot = fileName.lastIndexOf('.');
    if (dot <= 0) {
      return fileName + ".pdf";
    }
    return fileName.substring(0, dot) + ".pdf";
  }

  private String normalizeEscapedLineBreaks(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return value
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        // Strip a LONE trailing backslash (single \, not \\) that AI appends as a line-break
        // marker in plain-text mode. (?<!\\) and (?!\\) ensure we do NOT strip LaTeX \\
        // (double-backslash used in align, tabular, etc.).
        .replaceAll("(?m)(?<!\\\\)\\\\(?!\\\\)\\h*$", "")
        .trim();
  }

  private String normalizeLatexInput(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    String normalized = value.replace("\\$", "$");
    normalized = normalized.replace("\\\\(", "\\(").replace("\\\\)", "\\)");
    return normalized.replace("\\\\[", "\\[").replace("\\\\]", "\\]");
  }

  private String sanitizeDisplayText(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    String withoutTextCommands =
        value.replaceAll("\\\\(?:textbf|textit|text|mathrm)\\{([^{}]+)}", "$1");
    String withoutSpacingMacros = withoutTextCommands.replaceAll("\\\\(?:quad|qquad)", " ");
    return withoutSpacingMacros.replaceAll(" {2,}", " ").trim();
  }

  private Map<String, String> buildSlidePlaceholders(int slideIndex, DeckSections deck) {
    Map<String, String> values = new HashMap<>();
    values.put("{{LESSON_TITLE}}", safe(deck.lessonTitle()));
    values.put("{{LESSON_SUMMARY}}", safe(deck.lessonSummary()));
    values.put("{{LEARNING_OBJECTIVES}}", safe(deck.learningObjectives()));
    values.put("{{ADDITIONAL_PROMPT}}", safe(deck.opening()));
    values.put("{{LESSON_CONTENT}}", safe(deck.fullLessonContent()));

    switch (slideIndex) {
      case 1 -> {
        values.put("{{LESSON_SUMMARY}}", safe(deck.coverSummary()));
      }
      case 2 -> {
        values.put("{{LEARNING_OBJECTIVES}}", safe(deck.learningObjectives()));
      }
      case 3 -> {
        values.put("{{ADDITIONAL_PROMPT}}", safe(deck.opening()));
      }
      case 4 -> {
        values.put("{{LESSON_CONTENT}}", safe(deck.mainPart1()));
      }
      case 5 -> {
        values.put("{{LESSON_CONTENT}}", safe(deck.mainPart2()));
      }
      case 6 -> {
        values.put("{{LESSON_CONTENT}}", safe(deck.mainPart3()));
      }
      case 7 -> {
        values.put("{{LESSON_CONTENT}}", safe(deck.examplePart()));
      }
      case 8 -> {
        values.put("{{LESSON_CONTENT}}", safe(deck.practicePart()));
      }
      case 9 -> {
        values.put("{{LESSON_SUMMARY}}", safe(deck.closingSummary()));
      }
      case 10 -> {
        values.put("{{LESSON_TITLE}}", safe(deck.lessonTitle()));
      }
      default -> {
        // Keep default mapping for templates with more than 10 slides.
      }
    }
    return values;
  }

  private byte[] injectTemplateWithJson(
      byte[] templateBytes,
      Lesson lesson,
      List<LessonSlideJsonItemRequest> slides,
      LessonSlideOutputFormat outputFormat) {
    try (XMLSlideShow slideshow = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      int templateSlideCount = slideshow.getSlides().size();
      int requestedSlideCount = slides == null ? 0 : slides.size();

      if (requestedSlideCount == 0) {
        throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
      }

      if (requestedSlideCount > templateSlideCount) {
        // Template has fewer slides than requested. Clone the first MAIN_CONTENT slide
        // (index 3 = slide 4 in a 10-slide template) to fill the gap.
        // The clones are inserted right after the existing MAIN_CONTENT block so that
        // EXAMPLE / PRACTICE / SUMMARY / CLOSING remain at the tail.
        int extraNeeded = requestedSlideCount - templateSlideCount;
        // Index 3 = MAIN_CONTENT slide 1 (0-based). Guard: use last slide if template < 4 slides.
        int contentTemplateIndex = Math.min(3, templateSlideCount - 1);
        XSLFSlide contentTemplate = slideshow.getSlides().get(contentTemplateIndex);
        for (int extra = 0; extra < extraNeeded; extra++) {
          XSLFSlide newSlide = slideshow.createSlide();
          newSlide.importContent(contentTemplate);
          // Insert right after contentTemplateIndex (shift subsequent extras forward).
          slideshow.setSlideOrder(newSlide, contentTemplateIndex + 1 + extra);
        }
        templateSlideCount = slideshow.getSlides().size(); // now == requestedSlideCount
        log.info(
            "Cloned {} extra MAIN_CONTENT slide(s) from template index {}. Total slides now: {}",
            extraNeeded, contentTemplateIndex, templateSlideCount);
      }

      boolean useFullLatexImage = outputFormat == LessonSlideOutputFormat.LATEX;

      int applyCount = requestedSlideCount;
      for (int i = 0; i < applyCount; i++) {
        XSLFSlide slide = slideshow.getSlides().get(i);
        LessonSlideJsonItemRequest item = slides.get(i);

        Map<String, String> slideValues = buildJsonSlidePlaceholders(item, lesson);

        if (useFullLatexImage) {
          // Step 1 (LATEX): Replace placeholders only on title/decoration shapes.
          // The main content shape is intentionally skipped here — step 2 will render
          // heading+content as a full PNG and inject it, so we must NOT pre-fill that
          // shape with text (which would cause text and image to appear simultaneously).
          XSLFTextShape contentShape = findLargestTextShape(slide);
          replacePlaceholdersInSlideExcept(slideshow, slide, slideValues, contentShape);

          // Step 2 (LATEX): Render entire content as one PNG image and inject it.
          applySlideAsFullLatexImage(slideshow, slide, item);
        } else {
          // PLAIN_TEXT / HYBRID: replace all placeholder tags in every shape.
          replacePlaceholdersInSlide(slideshow, slide, slideValues);
        }
      }

      // Keep output deck size aligned with requested slide count when template has more slides.
      for (int i = templateSlideCount - 1; i >= requestedSlideCount; i--) {
        slideshow.removeSlide(i);
      }

      slideshow.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception ex) {
      log.error(
          "Failed to generate pptx from confirmed json. lessonId={}, slideCount={}, reason={}",
          lesson.getId(),
          slides == null ? 0 : slides.size(),
          ex.getMessage(),
          ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  /**
   * Renders the entire content of a slide (heading + body LaTeX/TikZ) as a single PNG image via
   * QuickLaTeX, then injects that image into the largest content text-shape area of the slide,
   * hiding the original text shape. This enables full geometry (TikZ) rendering in PPTX output.
   */
  private void applySlideAsFullLatexImage(
      XMLSlideShow slideshow, XSLFSlide slide, LessonSlideJsonItemRequest item) {

    String heading = normalizeAiGeneratedText(item.getHeading());
    String content = normalizeAiGeneratedText(item.getContent());

    // Find the largest text shape that holds body content — we'll replace it with the image.
    XSLFTextShape targetShape = findLargestTextShape(slide);
    if (targetShape == null) {
      log.warn("No usable text shape found in slide {} for full-LaTeX injection", item.getSlideNumber());
      return;
    }

    byte[] imageBytes = renderSlideAsFullLatexImage(heading, content);
    if (imageBytes.length == 0) {
      // Render failed — placeholder text was already replaced by the normal loop in step 1.
      log.debug("LATEX render returned empty bytes for slide {}, keeping replaced text", item.getSlideNumber());
      return;
    }

    Rectangle2D anchor = targetShape.getAnchor();
    if (anchor == null) {
      log.warn("Text shape anchor is null for slide {}", item.getSlideNumber());
      return;
    }

    // Clear the target shape safely: clearText() removes all paragraphs, but OOXML requires
    // at least one <a:p> in a txBody — add an empty one to keep the XML valid.
    targetShape.clearText();
    targetShape.addNewTextParagraph();

    try {
      XSLFPictureData pictureData = slideshow.addPicture(imageBytes, PictureData.PictureType.PNG);
      XSLFPictureShape pictureShape = slide.createPicture(pictureData);
      // Expand width by 1 inch (72 pt) so the image fills more of the slide horizontally.
      Rectangle2D expandedAnchor = new Rectangle2D.Double(
          anchor.getX(), anchor.getY(), anchor.getWidth() + 72.0, anchor.getHeight());
      pictureShape.setAnchor(expandedAnchor);
    } catch (Exception ex) {
      log.warn("Failed to inject full-LaTeX image for slide {}: {}", item.getSlideNumber(), ex.getMessage());
    }
  }

  /**
   * Returns the text shape with the largest area that is likely the main content area.
   * Skips shapes that are probably title shapes (small height).
   */
  // Placeholders that mark a shape as the main slide CONTENT area.
  private static final List<String> CONTENT_PLACEHOLDER_KEYS = List.of(
      "{{LESSON_CONTENT}}", "{{LESSON_SUMMARY}}", "{{SLIDE_CONTENT}}",
      "{{LEARNING_OBJECTIVES}}", "{{ADDITIONAL_PROMPT}}");

  /**
   * Finds the best target shape for LATEX full-image injection.
   *
   * <p>Priority:
   * <ol>
   *   <li>A shape whose text contains a recognised content placeholder tag
   *       ({@code {{LESSON_CONTENT}}}, {@code {{LESSON_SUMMARY}}}, etc.).
   *   <li>Fallback: the text shape with the largest area that does NOT contain
   *       {@code {{LESSON_TITLE}}} — so title shapes are never hijacked as content areas.
   * </ol>
   */
  private XSLFTextShape findLargestTextShape(XSLFSlide slide) {
    XSLFTextShape contentTagged = null;
    XSLFTextShape largest = null;
    double bestArea = 0;

    for (XSLFShape shape : slide.getShapes()) {
      if (!(shape instanceof XSLFTextShape ts)) continue;
      Rectangle2D anchor = ts.getAnchor();
      if (anchor == null) continue;

      String text = safe(ts.getText());

      // Prefer a shape explicitly tagged as a content area.
      if (contentTagged == null) {
        for (String key : CONTENT_PLACEHOLDER_KEYS) {
          if (text.contains(key)) {
            contentTagged = ts;
            break;
          }
        }
      }

      // Track largest non-title shape as fallback.
      if (!text.contains("{{LESSON_TITLE}}")) {
        double area = anchor.getWidth() * anchor.getHeight();
        if (area > bestArea) {
          bestArea = area;
          largest = ts;
        }
      }
    }

    return contentTagged != null ? contentTagged : largest;
  }

  /**
   * Builds a self-contained LaTeX body combining heading and content, then renders it via
   * QuickLaTeX (mode=1 for full-document support including TikZ geometry). Returns the PNG bytes.
   */
  private byte[] renderSlideAsFullLatexImage(String heading, String content) {
    String imageUrl = renderSlidePreviewUrl(heading, content);
    if (imageUrl == null) return new byte[0];
    return downloadImageBytes(imageUrl);
  }

  /**
   * Renders heading + content via QuickLaTeX and returns the hosted image URL.
   * Returns null on failure.
   */
  private String renderSlidePreviewUrl(String heading, String content) {
    String latexBody = buildLatexSlideBody(heading, content);
    try {
      LatexRenderRequest renderRequest =
          LatexRenderRequest.builder()
              .latex(latexBody)
              .options(
                  LatexRenderRequest.LatexRenderOptions.builder()
                      .mode(1) // Full LaTeX body mode — supports TikZ, pgfplots, etc.
                      .preamble(QUICKLATEX_PREAMBLE)
                      .fontSize("14px")
                      .build())
              .build();
      return latexRenderService.render(renderRequest);
    } catch (Exception ex) {
      log.warn("Full-LaTeX slide preview render failed (heading={}): {}", heading, ex.getMessage());
      return null;
    }
  }

  /**
   * Wraps heading and body content into a LaTeX snippet suitable for QuickLaTeX mode=1.
   * Plain text lines are wrapped in \text{} so they render correctly alongside math/TikZ.
   */
  private String buildLatexSlideBody(String heading, String body) {
    StringBuilder sb = new StringBuilder();
    if (heading != null && !heading.isBlank()) {
      sb.append("{\\Large\\textbf{").append(escapeLatexText(heading)).append("}}\\\\[6pt]\n");
    }
    if (body == null || body.isBlank()) {
      return sb.toString().isBlank() ? "\\phantom{x}" : sb.toString();
    }

    // If the body already contains a LaTeX block environment (\begin{...}) emit
    // it verbatim — do NOT split line-by-line, otherwise \item / \textbf etc.
    // would be incorrectly escaped by escapeLatexText().
    if (body.contains("\\begin{")) {
      sb.append(body.trim()).append("\n");
      return sb.toString();
    }

    // Line-by-line processing for plain / inline-math content.
    for (String line : body.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        sb.append("\\\\[4pt]\n");
        continue;
      }
      if (isBlockLatexLine(trimmed)) {
        sb.append(trimmed).append("\\\\[4pt]\n");
      } else {
        // Mixed line: might contain plain text, inline $...$, or both.
        // Wrap text segments in \text{} and keep $...$ math segments as-is.
        sb.append(buildMixedLatexLine(trimmed)).append("\\\\[4pt]\n");
      }
    }
    return sb.toString();
  }

  /**
   * Returns true only for block-level LaTeX constructs (environments, display math).
   * Inline math ($...$) is handled by buildMixedLatexLine instead.
   */
  private boolean isBlockLatexLine(String line) {
    return line.contains("\\begin{") || line.contains("\\end{")
        || line.contains("\\[")
        || line.contains("\\]");
  }

  /**
   * Converts a line that may contain a mix of plain text and inline {@code $...$} math into
   * a LaTeX fragment where text segments are wrapped in {@code \text{}} and math segments
   * are emitted verbatim.
   *
   * <p>Examples:
   * <ul>
   *   <li>{@code "abc"} → {@code \text{abc}}
   *   <li>{@code "Prove $a+b=c$"} → {@code \text{Prove }$a+b=c$}
   *   <li>{@code "$x^2$ and $y^2$"} → {@code $x^2$\text{ and }$y^2$}
   * </ul>
   */
  private String buildMixedLatexLine(String line) {
    if (!line.contains("$")) {
      return "\\text{" + escapeLatexText(line) + "}";
    }
    StringBuilder sb = new StringBuilder();
    int i = 0;
    boolean changed = false;
    while (i < line.length()) {
      int dollarStart = line.indexOf('$', i);
      if (dollarStart == -1) {
        // Remaining text
        sb.append("\\text{").append(escapeLatexText(line.substring(i))).append("}");
        changed = true;
        break;
      }
      // Text before $
      if (dollarStart > i) {
        sb.append("\\text{").append(escapeLatexText(line.substring(i, dollarStart))).append("}");
        changed = true;
      }
      // Find closing $
      int dollarEnd = line.indexOf('$', dollarStart + 1);
      if (dollarEnd == -1) {
        // Unclosed $: treat everything from here as text
        sb.append("\\text{").append(escapeLatexText(line.substring(dollarStart))).append("}");
        changed = true;
        break;
      }
      // Emit $...$ verbatim
      sb.append(line, dollarStart, dollarEnd + 1);
      i = dollarEnd + 1;
    }
    return changed ? sb.toString() : "\\text{" + escapeLatexText(line) + "}";
  }

  private String escapeLatexText(String text) {
    return text
        .replace("\\", "\\textbackslash{}")
        .replace("&", "\\&")
        .replace("%", "\\%")
        .replace("$", "\\$")
        .replace("#", "\\#")
        .replace("_", "\\_")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("~", "\\textasciitilde{}")
        .replace("^", "\\textasciicircum{}");
  }

  /** Downloads the image at the given URL and returns its raw bytes, or empty array on failure. */
  private byte[] downloadImageBytes(String imageUrl) {
    try {
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(imageUrl))
              .timeout(Duration.ofSeconds(15))
              .GET()
              .build();
      HttpResponse<byte[]> response =
          IMAGE_HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return response.body();
      }
      log.warn("Image download non-2xx: url={} status={}", imageUrl, response.statusCode());
      return new byte[0];
    } catch (Exception ex) {
      log.warn("Failed to download image from {}: {}", imageUrl, ex.getMessage());
      return new byte[0];
    }
  }

  private void applyTextAndLatex(
      XMLSlideShow slideshow, XSLFSlide slide, XSLFTextShape textShape, String content) {
    String normalizedContent = normalizeLatexInput(content);
    List<LatexToken> latexTokens = new ArrayList<>();
    String contentWithMarkers = replaceLatexWithMarkers(normalizedContent, latexTokens);
    String displayText =
      sanitizeDisplayText(
        contentWithMarkers.replaceAll("\\[\\[LATEX_\\d+\\]\\]", LATEX_INLINE_PLACEHOLDER));

    textShape.clearText();
    textShape.setText(displayText);

    if (latexTokens.isEmpty()) {
      return;
    }

    Rectangle2D anchor = textShape.getAnchor();
    if (anchor == null) {
      log.debug("Skip LaTeX image insertion because text shape anchor is null");
      return;
    }

    Matcher markerMatcher = Pattern.compile("\\[\\[LATEX_(\\d+)\\]\\]").matcher(contentWithMarkers);
    int pointer = 0;
    int line = 0;
    int col = 0;

    while (markerMatcher.find()) {
      String segment = contentWithMarkers.substring(pointer, markerMatcher.start());
      for (int i = 0; i < segment.length(); i++) {
        if (segment.charAt(i) == '\n') {
          line++;
          col = 0;
        } else {
          col++;
        }
      }

      int tokenIndex = Integer.parseInt(markerMatcher.group(1));
      if (tokenIndex < 0 || tokenIndex >= latexTokens.size()) {
        pointer = markerMatcher.end();
        continue;
      }

      LatexToken token = latexTokens.get(tokenIndex);
      byte[] imageBytes = renderLatexToPng(token.expression());
      if (imageBytes.length == 0) {
        pointer = markerMatcher.end();
        continue;
      }

      try (ByteArrayInputStream imageInput = new ByteArrayInputStream(imageBytes)) {
        BufferedImage sourceImage = ImageIO.read(imageInput);
        if (sourceImage == null) {
          pointer = markerMatcher.end();
          continue;
        }

        double width = sourceImage.getWidth();
        double height = sourceImage.getHeight();
        if (width <= 0 || height <= 0) {
          pointer = markerMatcher.end();
          continue;
        }

        double x = anchor.getX() + LATEX_HORIZONTAL_PADDING + col * LATEX_ESTIMATED_CHAR_WIDTH;
        double y = anchor.getY() + LATEX_VERTICAL_PADDING + line * LATEX_ESTIMATED_LINE_HEIGHT;
        double remainingWidth = anchor.getX() + anchor.getWidth() - LATEX_HORIZONTAL_PADDING - x;

        if (remainingWidth < 40d) {
          line++;
          col = 0;
          x = anchor.getX() + LATEX_HORIZONTAL_PADDING;
          y = anchor.getY() + LATEX_VERTICAL_PADDING + line * LATEX_ESTIMATED_LINE_HEIGHT;
          remainingWidth = Math.max(40d, anchor.getWidth() - LATEX_HORIZONTAL_PADDING * 2);
        }

        double widthScale = remainingWidth / width;
        double inlineMaxHeight = Math.max(12d, LATEX_ESTIMATED_LINE_HEIGHT * 0.9);
        double heightScale = Math.min(LATEX_MAX_IMAGE_HEIGHT, inlineMaxHeight) / height;
        double scale = Math.min(1d, Math.min(widthScale, heightScale));

        double renderWidth = Math.max(16d, width * scale);
        double renderHeight = Math.max(12d, Math.min(inlineMaxHeight, height * scale));
        double yOffset = Math.max(0d, (LATEX_ESTIMATED_LINE_HEIGHT - renderHeight) / 2d);

        XSLFPictureData pictureData = slideshow.addPicture(imageBytes, PictureData.PictureType.PNG);
        XSLFPictureShape pictureShape = slide.createPicture(pictureData);
        pictureShape.setAnchor(new Rectangle2D.Double(x, y + yOffset, renderWidth, renderHeight));

        col += LATEX_INLINE_PLACEHOLDER.length();
      } catch (Exception ex) {
        log.warn("Failed to append LaTeX image for expression: {}", token.expression(), ex);
      }

      pointer = markerMatcher.end();
    }
  }

  private String replaceLatexWithMarkers(String content, List<LatexToken> latexTokens) {
    Matcher matcher = LATEX_SEGMENT_PATTERN.matcher(content);
    StringBuffer out = new StringBuffer();

    while (matcher.find()) {
      String expression =
          firstNonBlank(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
      if (expression == null || expression.isBlank()) {
        continue;
      }

      LatexToken token = new LatexToken(expression.trim());
      latexTokens.add(token);
      matcher.appendReplacement(
          out, Matcher.quoteReplacement("[[LATEX_" + (latexTokens.size() - 1) + "]]"));
    }
    matcher.appendTail(out);

    return out.toString();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private byte[] renderLatexToPng(String expression) {
    try {
      TeXFormula formula = new TeXFormula(expression);
      TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, LATEX_FONT_SIZE);
      int width = Math.max(1, icon.getIconWidth());
      int height = Math.max(1, icon.getIconHeight());

      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = image.createGraphics();
      graphics.setColor(new Color(255, 255, 255, 0));
      graphics.fillRect(0, 0, width, height);
      graphics.setColor(Color.BLACK);
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      icon.paintIcon(new JLabel(), graphics, 0, 0);
      graphics.dispose();

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(image, "png", output);
      return output.toByteArray();
    } catch (Exception ex) {
      log.warn("Failed to render LaTeX expression: {}", expression, ex);
      return new byte[0];
    }
  }

  private record LatexToken(String expression) {}

  private Map<String, String> buildJsonSlidePlaceholders(
      LessonSlideJsonItemRequest item, Lesson lesson) {
    Map<String, String> values = new HashMap<>();
    String slideType = safe(item.getSlideType()).toUpperCase();
    String heading = safe(item.getHeading());
    String content = safe(item.getContent());

    // Fill all common placeholders with a sensible default body so non-10-slide flows
    // can still inject correctly into 10-slide templates.
    String titleValue = heading.isBlank() ? safe(lesson.getTitle()) : heading;

    values.put("{{SLIDE_HEADING}}", titleValue);
    values.put("{{SLIDE_CONTENT}}", content);
    values.put("{{LESSON_TITLE}}", titleValue);
    values.put("{{LESSON_SUMMARY}}", content);
    values.put("{{LEARNING_OBJECTIVES}}", content);
    values.put("{{ADDITIONAL_PROMPT}}", content);
    values.put("{{LESSON_CONTENT}}", content);

    switch (slideType) {
      case "COVER" -> {
        values.put("{{LESSON_TITLE}}", safe(lesson.getTitle()));
        values.put("{{LESSON_SUMMARY}}", content);
      }
      case "OBJECTIVES" -> values.put("{{LEARNING_OBJECTIVES}}", content);
      case "OPENING" -> values.put("{{ADDITIONAL_PROMPT}}", content);
      case "SUMMARY" -> values.put("{{LESSON_SUMMARY}}", content);
      case "CLOSING" -> values.put("{{LESSON_TITLE}}", safe(lesson.getTitle()));
      default -> {
        // MAIN_CONTENT / EXAMPLE / PRACTICE / CUSTOM use LESSON_CONTENT by default.
      }
    }
    return values;
  }

  private List<LessonSlideJsonItemResponse> buildPreviewSlides(
      DeckSections deck, int slideCount, LessonSlideOutputFormat outputFormat) {
    List<LessonSlideJsonItemResponse> slides = new ArrayList<>();

    // 10-slide canonical structure.
    if (slideCount == 10) {
      slides.add(buildPreviewSlide(1,  "COVER",        "Cover",            deck.coverSummary(),        outputFormat));
      slides.add(buildPreviewSlide(2,  "OBJECTIVES",   "Muc tieu",         deck.learningObjectives(),  outputFormat));
      slides.add(buildPreviewSlide(3,  "OPENING",      "Khoi dong",        deck.opening(),             outputFormat));
      slides.add(buildPreviewSlide(4,  "MAIN_CONTENT", "Noi dung chinh 1", deck.mainPart1(),           outputFormat));
      slides.add(buildPreviewSlide(5,  "MAIN_CONTENT", "Noi dung chinh 2", deck.mainPart2(),           outputFormat));
      slides.add(buildPreviewSlide(6,  "MAIN_CONTENT", "Noi dung chinh 3", deck.mainPart3(),           outputFormat));
      slides.add(buildPreviewSlide(7,  "EXAMPLE",      "Vi du",            deck.examplePart(),         outputFormat));
      slides.add(buildPreviewSlide(8,  "PRACTICE",     "Luyen tap",        deck.practicePart(),        outputFormat));
      slides.add(buildPreviewSlide(9,  "SUMMARY",      "Tong ket",         deck.closingSummary(),      outputFormat));
      slides.add(buildPreviewSlide(10, "CLOSING",      "Cam on / Q&A",     deck.lessonTitle(),         outputFormat));
      return slides;
    }

    int normalizedCount = Math.max(5, Math.min(15, slideCount));
    int middleCount = normalizedCount - 5;
    List<String> middleChunks = splitIntoChunks(deck.fullLessonContent(), middleCount);

    slides.add(buildPreviewSlide(1, "COVER",      "Cover",     deck.coverSummary(),       outputFormat));
    slides.add(buildPreviewSlide(2, "OBJECTIVES", "Muc tieu",  deck.learningObjectives(), outputFormat));
    slides.add(buildPreviewSlide(3, "OPENING",    "Khoi dong", deck.opening(),            outputFormat));

    for (int i = 0; i < middleChunks.size(); i++) {
      String type = "MAIN_CONTENT";
      String heading = "Noi dung chinh " + (i + 1);
      if (i == middleChunks.size() - 2) {
        type = "EXAMPLE";
        heading = "Vi du";
      }
      if (i == middleChunks.size() - 1) {
        type = "PRACTICE";
        heading = "Luyen tap";
      }
      slides.add(buildPreviewSlide(4 + i, type, heading, middleChunks.get(i), outputFormat));
    }

    slides.add(buildPreviewSlide(normalizedCount - 1, "SUMMARY", "Tong ket",      deck.closingSummary(), outputFormat));
    slides.add(buildPreviewSlide(normalizedCount,     "CLOSING", "Cam on / Q&A",  deck.lessonTitle(),    outputFormat));
    return slides;
  }

  private LessonSlideJsonItemResponse buildPreviewSlide(
      int slideNumber, String slideType, String heading, String content,
      LessonSlideOutputFormat outputFormat) {
    String normalizedContent = normalizeAiGeneratedText(content);
    String normalizedHeading = normalizeAiGeneratedText(heading);

    String previewImageUrl = null;
    if (outputFormat == LessonSlideOutputFormat.LATEX) {
      previewImageUrl = renderSlidePreviewUrl(normalizedHeading, normalizedContent);
      if (previewImageUrl != null) {
        log.info("Slide {} ({}) preview URL generated: {}", slideNumber, slideType, previewImageUrl);
      } else {
        log.warn("Slide {} ({}) preview URL is NULL — QuickLaTeX render failed", slideNumber, slideType);
      }
    }

    return LessonSlideJsonItemResponse.builder()
        .slideNumber(slideNumber)
        .slideType(slideType)
        .heading(normalizedHeading)
        .content(normalizedContent)
        .previewImageUrl(previewImageUrl)
        .build();
  }

  private List<String> splitIntoChunks(String content, int chunkCount) {
    if (chunkCount <= 0) {
      return List.of();
    }

    List<String> paragraphs = new ArrayList<>();
    if (content != null && !content.isBlank()) {
      for (String line : content.split("\\r?\\n")) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.isBlank()) {
          paragraphs.add(trimmed);
        }
      }
    }

    if (paragraphs.isEmpty()) {
      List<String> empty = new ArrayList<>();
      for (int i = 0; i < chunkCount; i++) {
        empty.add("");
      }
      return empty;
    }

    int base = paragraphs.size() / chunkCount;
    int remainder = paragraphs.size() % chunkCount;
    List<String> result = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < chunkCount; i++) {
      int size = base + (i < remainder ? 1 : 0);
      if (size <= 0) {
        result.add(paragraphs.get(paragraphs.size() - 1));
      } else {
        List<String> slice = paragraphs.subList(start, start + size);
        result.add(String.join("\n", slice));
        start += size;
      }
    }

    return result;
  }

  private DeckSections buildDeckSections(Lesson lesson, String additionalPrompt) {
    String lessonContent = safe(lesson.getLessonContent());
    String summary =
        lesson.getSummary() == null || lesson.getSummary().isBlank()
            ? buildSummary(lessonContent)
            : lesson.getSummary();

    List<String> chunks = splitIntoFiveChunks(lessonContent);
    String opening =
        additionalPrompt != null && !additionalPrompt.isBlank()
            ? additionalPrompt
            : "Khoi dong bai hoc voi mot cau hoi goi mo lien quan den noi dung.";

    return new DeckSections(
        safe(lesson.getTitle()),
        safe(summary),
        safe(summary),
        safe(lesson.getLearningObjectives()),
        safe(opening),
        safe(chunks.get(0)),
        safe(chunks.get(1)),
        safe(chunks.get(2)),
        safe(chunks.get(3)),
        safe(chunks.get(4)),
        safe(summary),
        lessonContent);
  }

    private DeckSections buildDeckSectionsWithAi(
      Lesson lesson,
      String additionalPrompt,
      LessonSlideOutputFormat outputFormat,
      int requestedSlideCount) {
    DeckSections fallback = buildDeckSections(lesson, additionalPrompt);

    try {
        String prompt =
          buildSlideDeckPrompt(lesson, additionalPrompt, outputFormat, requestedSlideCount);
      String aiResponse = geminiService.sendMessage(prompt);
      AiDeckSections aiDeck = parseAiDeckSections(aiResponse);
      if (aiDeck == null || !hasEnoughAiContent(aiDeck)) {
        String taggedPrompt =
          buildSlideDeckTaggedPrompt(
            lesson, additionalPrompt, outputFormat, requestedSlideCount);
        String taggedResponse = geminiService.sendMessage(taggedPrompt);
        AiDeckSections taggedDeck = parseTaggedDeckSections(taggedResponse);
        if (taggedDeck != null && hasEnoughAiContent(taggedDeck)) {
          aiDeck = taggedDeck;
        }
      }
      if (aiDeck == null || !hasEnoughAiContent(aiDeck)) {
        log.warn("Gemini did not return usable slide deck content. Falling back to lesson data");
        return fallback;
      }

      String aiSummary = nonBlankOrDefault(aiDeck.lessonSummary, fallback.lessonSummary());
      String aiObjectives =
          nonBlankOrDefault(aiDeck.learningObjectives, fallback.learningObjectives());
      String aiOpening = nonBlankOrDefault(aiDeck.opening, fallback.opening());
      String aiMainPart1 = nonBlankOrDefault(aiDeck.mainPart1, fallback.mainPart1());
      String aiMainPart2 = nonBlankOrDefault(aiDeck.mainPart2, fallback.mainPart2());
      String aiMainPart3 = nonBlankOrDefault(aiDeck.mainPart3, fallback.mainPart3());
      String aiExample = nonBlankOrDefault(aiDeck.examplePart, fallback.examplePart());
      String aiPractice = nonBlankOrDefault(aiDeck.practicePart, fallback.practicePart());
      String aiClosing = nonBlankOrDefault(aiDeck.closingSummary, fallback.closingSummary());

      String aiFullContent =
          joinSections(
              aiMainPart1,
              aiMainPart2,
              aiMainPart3,
              aiExample,
              aiPractice,
              aiClosing,
              nonBlankOrDefault(aiDeck.additionalNotes, ""));

      return new DeckSections(
          safe(lesson.getTitle()),
          aiSummary,
          aiSummary,
          aiObjectives,
          aiOpening,
          aiMainPart1,
          aiMainPart2,
          aiMainPart3,
          aiExample,
          aiPractice,
          aiClosing,
          nonBlankOrDefault(aiFullContent, fallback.fullLessonContent()));
    } catch (Exception ex) {
      log.warn("Gemini slide content generation failed, fallback to lesson data", ex);
      return fallback;
    }
  }

  private boolean hasEnoughAiContent(AiDeckSections aiDeck) {
    int filled = 0;
    if (aiDeck.lessonSummary != null && !aiDeck.lessonSummary.isBlank()) {
      filled++;
    }
    if (aiDeck.learningObjectives != null && !aiDeck.learningObjectives.isBlank()) {
      filled++;
    }
    if (aiDeck.mainPart1 != null && !aiDeck.mainPart1.isBlank()) {
      filled++;
    }
    if (aiDeck.mainPart2 != null && !aiDeck.mainPart2.isBlank()) {
      filled++;
    }
    if (aiDeck.mainPart3 != null && !aiDeck.mainPart3.isBlank()) {
      filled++;
    }
    if (aiDeck.examplePart != null && !aiDeck.examplePart.isBlank()) {
      filled++;
    }
    if (aiDeck.practicePart != null && !aiDeck.practicePart.isBlank()) {
      filled++;
    }
    if (aiDeck.closingSummary != null && !aiDeck.closingSummary.isBlank()) {
      filled++;
    }
    return filled >= 6;
  }

  private AiDeckSections parseAiDeckSections(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    try {
      String cleaned = raw.trim();
      if (cleaned.startsWith("```json")) {
        cleaned = cleaned.substring(7);
      } else if (cleaned.startsWith("```")) {
        cleaned = cleaned.substring(3);
      }
      if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length() - 3);
      }
      cleaned = cleaned.trim();

      int start = cleaned.indexOf('{');
      int end = cleaned.lastIndexOf('}');
      if (start >= 0 && end > start) {
        cleaned = cleaned.substring(start, end + 1);
      }

      return objectMapper.readValue(cleaned, AiDeckSections.class);
    } catch (Exception ex) {
      log.warn("Failed to parse AI deck sections response", ex);
      return null;
    }
  }

    private String buildSlideDeckPrompt(
      Lesson lesson,
      String additionalPrompt,
      LessonSlideOutputFormat outputFormat,
      int requestedSlideCount) {
    String lessonTitle = safe(lesson.getTitle());
    String lessonContent = safe(lesson.getLessonContent());
    String lessonSummary = safe(lesson.getSummary());
    String learningObjectives = safe(lesson.getLearningObjectives());
    String opening = safe(additionalPrompt);
    String formatGuidance = buildFormatGuidance(outputFormat, true);

    return """
        Bạn là giáo viên Toán THCS/THPT tại Việt Nam.
        Hãy soạn nội dung trình chiếu đầy đủ cho bài học theo cấu trúc %d slide.

        THÔNG TIN ĐẦU VÀO:
        - lessonTitle: %s
        - existingSummary: %s
        - existingLearningObjectives: %s
        - additionalPrompt: %s
        - rawLessonContent:
        %s

        YÊU CẦU:
        - Viết bằng tiếng Việt có dấu.
        - Nội dung phải NGẮN GỌN cho đúng phạm vi 1 slide nhưng vẫn TOÀN VẸN ý chính.
        - Mỗi phần (opening/mainPart/example/practice/closing) chỉ 3-5 ý chính.
        - Mỗi ý tối đa khoảng 12-16 từ, ưu tiên câu ngắn và rõ nghĩa.
        - Tránh văn xuôi dài; ưu tiên gạch đầu dòng hoặc câu ngắn tách dòng.
        - Tổng độ dài mỗi phần nên trong khoảng 60-90 từ, không lan man.
        - Nội dung phải cụ thể, không lặp lại cùng một câu giữa các phần.
        - Mỗi mục mainPart1/mainPart2/mainPart3/examplePart/practicePart cần có nhiều ý (dạng gạch đầu dòng ngắn, xuống dòng THẬT bằng Enter).
        - Không ghi literal \n hoặc /n trong nội dung.
        - closingSummary phải tóm tắt được kiến thức trọng tâm và nhấn mạnh cách áp dụng.
        - outputFormat: %s
        %s
        - Không thêm markdown, không thêm giải thích ngoài JSON.

        Trả về CHI DUY NHAT mot JSON object dung schema sau:
        {
          "lessonSummary": "...",
          "learningObjectives": "...",
          "opening": "...",
          "mainPart1": "...",
          "mainPart2": "...",
          "mainPart3": "...",
          "examplePart": "...",
          "practicePart": "...",
          "closingSummary": "...",
          "additionalNotes": "..."
        }
        """
        .formatted(
          requestedSlideCount,
            lessonTitle,
            lessonSummary,
            learningObjectives,
            opening,
            lessonContent,
            outputFormat,
            formatGuidance);
  }

  private String buildSlideDeckTaggedPrompt(
        Lesson lesson,
        String additionalPrompt,
        LessonSlideOutputFormat outputFormat,
        int requestedSlideCount) {
    String lessonTitle = safe(lesson.getTitle());
    String lessonContent = safe(lesson.getLessonContent());
    String lessonSummary = safe(lesson.getSummary());
    String learningObjectives = safe(lesson.getLearningObjectives());
    String opening = safe(additionalPrompt);
    String formatGuidance = buildFormatGuidance(outputFormat, false);

    return """
        Bạn là giáo viên Toán THCS/THPT tại Việt Nam.
        Hãy soạn nội dung đầy đủ cho bài trình chiếu %d slide.

        THÔNG TIN ĐẦU VÀO:
        - lessonTitle: %s
        - existingSummary: %s
        - existingLearningObjectives: %s
        - additionalPrompt: %s
        - rawLessonContent:
        %s

        YÊU CẦU:
        - Viết tiếng Việt có dấu, nội dung cụ thể, không lặp lại giữa các phần.
        - Nội dung phải NGẮN GỌN theo chuẩn 1 slide nhưng vẫn đầy đủ ý cốt lõi.
        - Mỗi phần chỉ 3-5 ý chính, mỗi ý tối đa khoảng 12-16 từ.
        - Ưu tiên gạch đầu dòng/câu ngắn, tránh đoạn văn dài.
        - Tổng độ dài mỗi phần nên trong khoảng 60-90 từ.
        - Mỗi phần mainPart1/mainPart2/mainPart3/examplePart/practicePart có nhiều ý và xuống dòng rõ ràng.
        - Không ghi literal \n hoặc /n trong nội dung.
        - outputFormat: %s
        %s
        - Chỉ trả về đúng định dạng marker dưới đây, không thêm markdown.

        [LESSON_SUMMARY]
        ...
        [LEARNING_OBJECTIVES]
        ...
        [OPENING]
        ...
        [MAIN_PART_1]
        ...
        [MAIN_PART_2]
        ...
        [MAIN_PART_3]
        ...
        [EXAMPLE_PART]
        ...
        [PRACTICE_PART]
        ...
        [CLOSING_SUMMARY]
        ...
        [ADDITIONAL_NOTES]
        ...
        """
        .formatted(
          requestedSlideCount,
          lessonTitle,
          lessonSummary,
          learningObjectives,
          opening,
          lessonContent,
          outputFormat,
          formatGuidance);
  }

  private AiDeckSections parseTaggedDeckSections(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    AiDeckSections deck = new AiDeckSections();
    deck.lessonSummary = extractTagged(raw, "LESSON_SUMMARY");
    deck.learningObjectives = extractTagged(raw, "LEARNING_OBJECTIVES");
    deck.opening = extractTagged(raw, "OPENING");
    deck.mainPart1 = extractTagged(raw, "MAIN_PART_1");
    deck.mainPart2 = extractTagged(raw, "MAIN_PART_2");
    deck.mainPart3 = extractTagged(raw, "MAIN_PART_3");
    deck.examplePart = extractTagged(raw, "EXAMPLE_PART");
    deck.practicePart = extractTagged(raw, "PRACTICE_PART");
    deck.closingSummary = extractTagged(raw, "CLOSING_SUMMARY");
    deck.additionalNotes = extractTagged(raw, "ADDITIONAL_NOTES");

    return hasEnoughAiContent(deck) ? deck : null;
  }

  private String extractTagged(String raw, String tag) {
    String startTag = "[" + tag + "]";
    int start = raw.indexOf(startTag);
    if (start < 0) {
      return "";
    }

    int contentStart = start + startTag.length();
    int nextTagStart = findNextTaggedSectionStart(raw, contentStart);

    String value = raw.substring(contentStart, nextTagStart).trim();
    if (value.startsWith(":")) {
      value = value.substring(1).trim();
    }
    return normalizeAiGeneratedText(value);
  }

  private int findNextTaggedSectionStart(String raw, int fromIndex) {
    int nextTagStart = raw.length();
    for (String section : TAGGED_SECTIONS) {
      int candidateStart = raw.indexOf("[" + section + "]", fromIndex);
      if (candidateStart >= 0 && candidateStart < nextTagStart) {
        nextTagStart = candidateStart;
      }
    }
    return nextTagStart;
  }

  private LessonSlideOutputFormat resolveOutputFormat(LessonSlideOutputFormat outputFormat) {
    return outputFormat == null ? LessonSlideOutputFormat.PLAIN_TEXT : outputFormat;
  }

  private String buildFormatGuidance(LessonSlideOutputFormat outputFormat, boolean jsonMode) {
    return switch (outputFormat) {
      case LATEX -> jsonMode
          ? "- Tất cả biểu thức toán học PHẢI dùng LaTeX inline ($...$) hoặc display (\\\\[ ... \\\\]).\n"
              + "- Danh sách bước/ý: dùng \\\\begin{itemize} \\\\item ... \\\\end{itemize}. KHÔNG dùng dấu gạch đầu dòng thủ công.\n"
              + "- Vì output là JSON string, PHẢI escape backslash: \\\\frac, \\\\item, \\\\begin, \\\\end, \\\\textbf, v.v.\n"
              + "- KHÔNG thêm ký tự \\\\ ở cuối dòng văn bản thường.\n"
              + "- KHÔNG viết công thức dạng chữ thuần túy."
          : "- Tất cả biểu thức toán học PHẢI dùng LaTeX inline ($...$) hoặc display (\\[ ... \\]).\n"
              + "- Danh sách bước/ý: dùng \\begin{itemize} \\item ... \\end{itemize}. KHÔNG dùng dấu gạch đầu dòng thủ công.\n"
              + "- KHÔNG thêm ký tự \\ ở cuối dòng văn bản thường.\n"
              + "- KHÔNG viết công thức dạng chữ thuần túy.";
      case HYBRID -> jsonMode
          ? "- Nội dung mô tả bằng tiếng Việt, công thức/ký hiệu toán dùng LaTeX inline ($...$).\n"
              + "- Vì output là JSON string, PHẢI escape backslash: \\\\frac, \\\\sqrt, v.v.\n"
              + "- KHÔNG thêm ký tự \\\\ ở cuối dòng văn bản thường."
          : "- Nội dung mô tả bằng tiếng Việt, công thức/ký hiệu toán dùng LaTeX inline ($...$).\n"
              + "- KHÔNG thêm ký tự \\ ở cuối dòng văn bản thường.";
      case PLAIN_TEXT ->
          "- Dùng văn bản thuần, không bắt buộc LaTeX.\n"
              + "- Nếu có công thức thì giữ dạng dễ đọc bằng chữ.";
    };
  }

  private String nonBlankOrDefault(String value, String fallback) {
    String normalizedValue = normalizeAiGeneratedText(value);
    if (normalizedValue.isBlank()) {
      return normalizeAiGeneratedText(fallback);
    }
    return normalizedValue;
  }

  private String normalizeAiGeneratedText(String value) {
    String safeValue = safe(value);
    if (safeValue.isEmpty()) {
      return safeValue;
    }
    return normalizeEscapedLineBreaks(safeValue).trim();
  }

  private String joinSections(String... sections) {
    List<String> lines = new ArrayList<>();
    for (String section : sections) {
      if (section != null && !section.isBlank()) {
        lines.add(section.trim());
      }
    }
    return String.join("\n\n", lines);
  }

  private List<String> splitIntoFiveChunks(String content) {
    List<String> paragraphs = new ArrayList<>();
    if (content != null && !content.isBlank()) {
      for (String line : content.split("\\r?\\n")) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.isBlank()) {
          paragraphs.add(trimmed);
        }
      }
    }

    if (paragraphs.isEmpty()) {
      return List.of("", "", "", "", "");
    }

    if (paragraphs.size() >= 5) {
      int base = paragraphs.size() / 5;
      int remainder = paragraphs.size() % 5;
      List<String> result = new ArrayList<>();
      int start = 0;
      for (int i = 0; i < 5; i++) {
        int size = base + (i < remainder ? 1 : 0);
        if (size <= 0) {
          result.add("");
        } else {
          List<String> slice = paragraphs.subList(start, start + size);
          result.add(String.join("\n", slice));
          start += size;
        }
      }
      return result;
    }

    List<String> padded = new ArrayList<>(paragraphs);
    while (padded.size() < 5) {
      padded.add(padded.get(padded.size() - 1));
    }
    return padded;
  }

  private record DeckSections(
      String lessonTitle,
      String lessonSummary,
      String coverSummary,
      String learningObjectives,
      String opening,
      String mainPart1,
      String mainPart2,
      String mainPart3,
      String examplePart,
      String practicePart,
      String closingSummary,
      String fullLessonContent) {}

  private static class AiDeckSections {
    public String lessonSummary;
    public String learningObjectives;
    public String opening;
    public String mainPart1;
    public String mainPart2;
    public String mainPart3;
    public String examplePart;
    public String practicePart;
    public String closingSummary;
    public String additionalNotes;
  }

  private void ensureTemplateBucketExists() {
    try {
      boolean exists =
          minioClient.bucketExists(
              BucketExistsArgs.builder().bucket(minioProperties.getTemplateBucket()).build());
      if (!exists) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(minioProperties.getTemplateBucket()).build());
      }
    } catch (Exception ex) {
      log.error("Failed to ensure template bucket exists", ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private byte[] readObject(String bucket, String objectKey) {
    try {
      minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
      try (InputStream stream =
          minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
        return stream.readAllBytes();
      }
    } catch (Exception ex) {
      log.error("Failed to read object {} from bucket {}", objectKey, bucket, ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private void validatePptxFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || !fileName.toLowerCase().endsWith(".pptx")) {
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }
  }

  private void validatePreviewImage(MultipartFile previewImage) {
    if (previewImage == null || previewImage.isEmpty()) {
      return;
    }

    String fileName = previewImage.getOriginalFilename();
    String lowerFileName = fileName == null ? "" : fileName.toLowerCase();
    String contentType = previewImage.getContentType();

    boolean validExtension =
        lowerFileName.endsWith(".png")
            || lowerFileName.endsWith(".jpg")
            || lowerFileName.endsWith(".jpeg")
            || lowerFileName.endsWith(".webp");

    boolean validContentType =
        PNG_MIME.equalsIgnoreCase(contentType)
            || JPG_MIME.equalsIgnoreCase(contentType)
            || WEBP_MIME.equalsIgnoreCase(contentType);

    if (!validExtension && !validContentType) {
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }
  }

  private String buildObjectKey(String originalFileName) {
    String normalized =
        originalFileName == null
            ? "template.pptx"
            : Normalizer.normalize(originalFileName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String sanitized = NON_ALNUM.matcher(normalized).replaceAll("_");
    return "slide-templates/"
        + UUID.randomUUID()
        + "/"
        + System.currentTimeMillis()
        + "-"
        + sanitized;
  }

  private String buildPreviewImageObjectKey(String originalFileName) {
    String normalized =
        originalFileName == null
            ? "preview.png"
            : Normalizer.normalize(originalFileName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String sanitized = NON_ALNUM.matcher(normalized).replaceAll("_");
    return "slide-templates-preview/"
        + UUID.randomUUID()
        + "/"
        + System.currentTimeMillis()
        + "-"
        + sanitized;
  }

  private String resolvePreviewImageContentType(MultipartFile previewImage) {
    String contentType = previewImage.getContentType();
    if (contentType != null && !contentType.isBlank()) {
      return contentType;
    }

    String fileName = previewImage.getOriginalFilename();
    if (fileName == null) {
      return PNG_MIME;
    }

    String lower = fileName.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return JPG_MIME;
    }
    if (lower.endsWith(".webp")) {
      return WEBP_MIME;
    }
    return PNG_MIME;
  }

  private String buildOutputFileName(String lessonTitle) {
    String base = lessonTitle == null || lessonTitle.isBlank() ? "lesson" : lessonTitle;
    String normalized = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String sanitized = NON_ALNUM.matcher(normalized).replaceAll("_");
    return sanitized + "-slides.pptx";
  }

  private String buildGeneratedSlideThumbnailObjectKey(String originalFileName) {
    String normalized =
        originalFileName == null
            ? "thumbnail.png"
            : Normalizer.normalize(originalFileName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String sanitized = NON_ALNUM.matcher(normalized).replaceAll("_");
    return "generated-slide-thumbnails/"
        + UUID.randomUUID()
        + "/"
        + System.currentTimeMillis()
        + "-"
        + sanitized;
  }

  private String resolveImageContentTypeFromObjectKey(String objectKey) {
    if (objectKey == null) {
      return PNG_MIME;
    }
    String lower = objectKey.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return JPG_MIME;
    }
    if (lower.endsWith(".webp")) {
      return WEBP_MIME;
    }
    return PNG_MIME;
  }

  private void persistGeneratedSlideFile(
      Lesson lesson, SlideTemplate template, byte[] content, String outputName) {
    ensureTemplateBucketExists();

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    String objectKey =
        "generated-slides/"
            + currentUserId
            + "/"
            + UUID.randomUUID()
            + "-"
            + NON_ALNUM.matcher(outputName).replaceAll("_");

    try {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(minioProperties.getTemplateBucket())
              .object(objectKey)
              .contentType(PPTX_MIME)
              .stream(new ByteArrayInputStream(content), content.length, -1)
              .build());
    } catch (Exception ex) {
      log.error("Failed to store generated slide file", ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }

    LessonSlideGeneratedFile generatedFile =
        LessonSlideGeneratedFile.builder()
            .lessonId(lesson.getId())
            .templateId(template.getId())
            .bucketName(minioProperties.getTemplateBucket())
            .objectKey(objectKey)
            .fileName(outputName)
        .name(lesson.getTitle())
            .contentType(PPTX_MIME)
            .fileSizeBytes((long) content.length)
            .isPublic(Boolean.FALSE)
            .build();
    generatedFile.setCreatedBy(currentUserId);

    lessonSlideGeneratedFileRepository.save(generatedFile);
  }

  private String safe(String input) {
    return input == null ? "" : input;
  }

  private String buildSummary(String content) {
    if (content == null || content.length() <= 220) {
      return content;
    }
    return content.substring(0, 220) + "...";
  }

  private LessonResponse toLessonResponse(Lesson lesson) {
    return LessonResponse.builder()
        .id(lesson.getId())
        .chapterId(lesson.getChapterId())
        .title(lesson.getTitle())
        .learningObjectives(lesson.getLearningObjectives())
        .lessonContent(lesson.getLessonContent())
        .summary(lesson.getSummary())
        .orderIndex(lesson.getOrderIndex())
        .durationMinutes(lesson.getDurationMinutes())
        .difficulty(lesson.getDifficulty())
        .status(lesson.getStatus())
        .createdAt(lesson.getCreatedAt())
        .updatedAt(lesson.getUpdatedAt())
        .build();
  }

  private SlideTemplateResponse toTemplateResponse(SlideTemplate template) {
    return SlideTemplateResponse.builder()
        .id(template.getId())
        .name(template.getName())
        .description(template.getDescription())
        .originalFileName(template.getOriginalFileName())
        .contentType(template.getContentType())
        .previewImage(buildPreviewImagePath(template))
        .active(Boolean.TRUE.equals(template.getIsActive()))
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .build();
  }

        private LessonSlideGeneratedFileResponse toGeneratedFileResponse(LessonSlideGeneratedFile file) {
          return LessonSlideGeneratedFileResponse.builder()
          .id(file.getId())
          .lessonId(file.getLessonId())
          .templateId(file.getTemplateId())
          .name(file.getName())
          .thumbnail(buildGeneratedSlideThumbnailPath(file))
          .fileName(file.getFileName())
          .contentType(file.getContentType())
          .fileSizeBytes(file.getFileSizeBytes())
          .isPublic(Boolean.TRUE.equals(file.getIsPublic()))
          .publishedAt(file.getPublishedAt())
          .createdAt(file.getCreatedAt())
          .updatedAt(file.getUpdatedAt())
          .build();
        }

  private String buildGeneratedSlideThumbnailPath(LessonSlideGeneratedFile file) {
    if (file.getThumbnail() == null || file.getThumbnail().isBlank()) {
      return null;
    }
    if (Boolean.TRUE.equals(file.getIsPublic())) {
      return "/lesson-slides/public/generated/" + file.getId() + "/thumbnail-image";
    }
    return "/lesson-slides/generated/" + file.getId() + "/thumbnail-image";
  }

  private String buildPreviewImagePath(SlideTemplate template) {
    if (template.getPreviewImageObjectKey() == null || template.getPreviewImageObjectKey().isBlank()) {
      return null;
    }
    return "/lesson-slides/templates/" + template.getId() + "/preview-image";
  }

  private void validateTeacherRole() {
    if (!SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }

  private void validateGeneratedFileOwnerOrAdmin(LessonSlideGeneratedFile file) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean isOwner = currentUserId.equals(file.getCreatedBy());
    boolean isAdmin = SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE);
    if (!isOwner && !isAdmin) {
      throw new AppException(ErrorCode.GENERATED_SLIDE_ACCESS_DENIED);
    }
  }
}
