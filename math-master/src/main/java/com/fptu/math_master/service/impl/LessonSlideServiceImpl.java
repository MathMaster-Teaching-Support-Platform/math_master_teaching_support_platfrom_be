package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.request.LessonSlideJsonItemRequest;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.LessonSlideJsonItemResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import com.fptu.math_master.enums.LessonSlideOutputFormat;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.SlideTemplate;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SlideTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.LessonSlideService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
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
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
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

  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;
  SlideTemplateRepository slideTemplateRepository;
  MinioClient minioClient;
  MinioProperties minioProperties;
  GeminiService geminiService;
  UserSubscriptionService userSubscriptionService;
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

    int slideCount = request.getSlideCount() == null ? 10 : request.getSlideCount();
  LessonSlideOutputFormat outputFormat = resolveOutputFormat(request.getOutputFormat());
  DeckSections deckSections =
    buildDeckSectionsWithAi(lesson, request.getAdditionalPrompt(), outputFormat);
    List<LessonSlideJsonItemResponse> slides = buildPreviewSlides(deckSections, slideCount);

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

    Lesson saved = lessonRepository.save(lesson);
    return toLessonResponse(saved);
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
  @Transactional(readOnly = true)
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
    return new BinaryFileData(generated, outputName, PPTX_MIME);
  }

  @Override
  @Transactional(readOnly = true)
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
    byte[] generated = injectTemplateWithJson(templateBytes, lesson, sortedSlides);
    String outputName = buildOutputFileName(lesson.getTitle());
    return new BinaryFileData(generated, outputName, PPTX_MIME);
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

  private String normalizeEscapedLineBreaks(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return value.replace("\\r\\n", "\n").replace("\\n", "\n");
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
      byte[] templateBytes, Lesson lesson, List<LessonSlideJsonItemRequest> slides) {
    try (XMLSlideShow slideshow = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      int applyCount = Math.min(slideshow.getSlides().size(), slides.size());
      for (int i = 0; i < applyCount; i++) {
        XSLFSlide slide = slideshow.getSlides().get(i);
        LessonSlideJsonItemRequest item = slides.get(i);
        Map<String, String> slideValues = buildJsonSlidePlaceholders(item, lesson);
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
      log.error(
          "Failed to generate pptx from confirmed json. lessonId={}, slideCount={}, reason={}",
          lesson.getId(),
          slides == null ? 0 : slides.size(),
          ex.getMessage(),
          ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private void applyTextAndLatex(
      XMLSlideShow slideshow, XSLFSlide slide, XSLFTextShape textShape, String content) {
    String normalizedContent = normalizeLatexInput(content);
    List<LatexToken> latexTokens = new ArrayList<>();
    String contentWithMarkers = replaceLatexWithMarkers(normalizedContent, latexTokens);
    String displayText = sanitizeDisplayText(contentWithMarkers.replaceAll("\\[\\[LATEX_\\d+\\]\\]", " "));

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
        double heightScale = LATEX_MAX_IMAGE_HEIGHT / height;
        double scale = Math.min(1d, Math.min(widthScale, heightScale));

        double renderWidth = Math.max(40d, width * scale);
        double renderHeight = Math.max(LATEX_MIN_IMAGE_HEIGHT, height * scale);

        XSLFPictureData pictureData = slideshow.addPicture(imageBytes, PictureData.PictureType.PNG);
        XSLFPictureShape pictureShape = slide.createPicture(pictureData);
        pictureShape.setAnchor(new Rectangle2D.Double(x, y, renderWidth, renderHeight));

        col += Math.max(1, (int) Math.ceil(renderWidth / LATEX_ESTIMATED_CHAR_WIDTH));
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

    values.put("{{SLIDE_HEADING}}", heading);
    values.put("{{SLIDE_CONTENT}}", content);
    values.put("{{LESSON_TITLE}}", safe(lesson.getTitle()));
    values.put("{{LESSON_SUMMARY}}", safe(lesson.getSummary()));
    values.put("{{LEARNING_OBJECTIVES}}", safe(lesson.getLearningObjectives()));
    values.put("{{ADDITIONAL_PROMPT}}", "");
    values.put("{{LESSON_CONTENT}}", content);

    switch (slideType) {
      case "COVER" -> values.put("{{LESSON_SUMMARY}}", content);
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

  private List<LessonSlideJsonItemResponse> buildPreviewSlides(DeckSections deck, int slideCount) {
    List<LessonSlideJsonItemResponse> slides = new ArrayList<>();

    // 10-slide canonical structure.
    if (slideCount == 10) {
      slides.add(buildPreviewSlide(1, "COVER", "Cover", deck.coverSummary()));
      slides.add(buildPreviewSlide(2, "OBJECTIVES", "Muc tieu", deck.learningObjectives()));
      slides.add(buildPreviewSlide(3, "OPENING", "Khoi dong", deck.opening()));
      slides.add(buildPreviewSlide(4, "MAIN_CONTENT", "Noi dung chinh 1", deck.mainPart1()));
      slides.add(buildPreviewSlide(5, "MAIN_CONTENT", "Noi dung chinh 2", deck.mainPart2()));
      slides.add(buildPreviewSlide(6, "MAIN_CONTENT", "Noi dung chinh 3", deck.mainPart3()));
      slides.add(buildPreviewSlide(7, "EXAMPLE", "Vi du", deck.examplePart()));
      slides.add(buildPreviewSlide(8, "PRACTICE", "Luyen tap", deck.practicePart()));
      slides.add(buildPreviewSlide(9, "SUMMARY", "Tong ket", deck.closingSummary()));
      slides.add(buildPreviewSlide(10, "CLOSING", "Cam on / Q&A", deck.lessonTitle()));
      return slides;
    }

    int normalizedCount = Math.max(5, Math.min(15, slideCount));
    int middleCount = normalizedCount - 5;
    List<String> middleChunks = splitIntoChunks(deck.fullLessonContent(), middleCount);

    slides.add(buildPreviewSlide(1, "COVER", "Cover", deck.coverSummary()));
    slides.add(buildPreviewSlide(2, "OBJECTIVES", "Muc tieu", deck.learningObjectives()));
    slides.add(buildPreviewSlide(3, "OPENING", "Khoi dong", deck.opening()));

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
      slides.add(buildPreviewSlide(4 + i, type, heading, middleChunks.get(i)));
    }

    slides.add(
        buildPreviewSlide(normalizedCount - 1, "SUMMARY", "Tong ket", deck.closingSummary()));
    slides.add(buildPreviewSlide(normalizedCount, "CLOSING", "Cam on / Q&A", deck.lessonTitle()));
    return slides;
  }

  private LessonSlideJsonItemResponse buildPreviewSlide(
      int slideNumber, String slideType, String heading, String content) {
    return LessonSlideJsonItemResponse.builder()
        .slideNumber(slideNumber)
        .slideType(slideType)
        .heading(heading)
        .content(safe(content))
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
      Lesson lesson, String additionalPrompt, LessonSlideOutputFormat outputFormat) {
    DeckSections fallback = buildDeckSections(lesson, additionalPrompt);

    try {
      String prompt = buildSlideDeckPrompt(lesson, additionalPrompt, outputFormat);
      String aiResponse = geminiService.sendMessage(prompt);
      AiDeckSections aiDeck = parseAiDeckSections(aiResponse);
      if (aiDeck == null || !hasEnoughAiContent(aiDeck)) {
        String taggedPrompt = buildSlideDeckTaggedPrompt(lesson, additionalPrompt, outputFormat);
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
      Lesson lesson, String additionalPrompt, LessonSlideOutputFormat outputFormat) {
    String lessonTitle = safe(lesson.getTitle());
    String lessonContent = safe(lesson.getLessonContent());
    String lessonSummary = safe(lesson.getSummary());
    String learningObjectives = safe(lesson.getLearningObjectives());
    String opening = safe(additionalPrompt);
    String formatGuidance = buildFormatGuidance(outputFormat, true);

    return """
        Bạn là giáo viên Toán THCS/THPT tại Việt Nam.
        Hãy soạn nội dung trình chiếu đầy đủ cho bài học theo cấu trúc 10 slide.

        THÔNG TIN ĐẦU VÀO:
        - lessonTitle: %s
        - existingSummary: %s
        - existingLearningObjectives: %s
        - additionalPrompt: %s
        - rawLessonContent:
        %s

        YÊU CẦU:
        - Viết bằng tiếng Việt có dấu.
        - Nội dung phải cụ thể, không lặp lại cùng một câu giữa các phần.
        - Mỗi mục mainPart1/mainPart2/mainPart3/examplePart/practicePart cần có nhiều ý (dạng gạch đầu dòng ngắn, tách dòng bằng ký tự xuống dòng \n).
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
            lessonTitle,
            lessonSummary,
            learningObjectives,
            opening,
            lessonContent,
            outputFormat,
            formatGuidance);
  }

  private String buildSlideDeckTaggedPrompt(
      Lesson lesson, String additionalPrompt, LessonSlideOutputFormat outputFormat) {
    String lessonTitle = safe(lesson.getTitle());
    String lessonContent = safe(lesson.getLessonContent());
    String lessonSummary = safe(lesson.getSummary());
    String learningObjectives = safe(lesson.getLearningObjectives());
    String opening = safe(additionalPrompt);
    String formatGuidance = buildFormatGuidance(outputFormat, false);

    return """
        Bạn là giáo viên Toán THCS/THPT tại Việt Nam.
        Hãy soạn nội dung đầy đủ cho bài trình chiếu 10 slide.

        THÔNG TIN ĐẦU VÀO:
        - lessonTitle: %s
        - existingSummary: %s
        - existingLearningObjectives: %s
        - additionalPrompt: %s
        - rawLessonContent:
        %s

        YÊU CẦU:
        - Viết tiếng Việt có dấu, nội dung cụ thể, không lặp lại giữa các phần.
        - Mỗi phần mainPart1/mainPart2/mainPart3/examplePart/practicePart có nhiều ý và xuống dòng rõ ràng.
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
    return value;
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
      case LATEX ->
          jsonMode
              ? "- Tất cả biểu thức toán học phải viết bằng LaTeX. KHÔNG dùng diễn đạt công thức dạng chữ thuần."
                  + "\\n- Vì output là JSON string, bắt buộc escape dấu \\\\ trong LaTeX (ví dụ: \\\\frac, \\\\sqrt, \\\\left, \\\\right)."
              : "- Tất cả biểu thức toán học phải viết bằng LaTeX."
                  + "\\n- Dùng delimiters chuẩn: inline \\( ... \\), block \\[ ... \\].";
      case HYBRID ->
          jsonMode
              ? "- Ưu tiên nội dung tiếng Việt dễ hiểu, nhưng công thức/ký hiệu toán phải dùng LaTeX."
                  + "\\n- Vì output là JSON string, bắt buộc escape dấu \\\\ trong LaTeX (ví dụ: \\\\frac, \\\\sqrt)."
              : "- Nội dung mô tả bằng tiếng Việt, công thức/ký hiệu toán thể hiện bằng LaTeX."
                  + "\\n- Dùng delimiters chuẩn: inline \\( ... \\), block \\[ ... \\].";
      case PLAIN_TEXT ->
          "- Dùng văn bản thuần như hiện tại, không bắt buộc LaTeX."
              + "\\n- Nếu có công thức thì giữ ở dạng dễ đọc bằng chữ.";
    };
  }

  private String nonBlankOrDefault(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return safe(fallback);
    }
    return value.trim();
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
}
