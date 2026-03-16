package com.fptu.math_master.service.impl;

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
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SlideTemplate;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SlideTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.LessonSlideService;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
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
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonSlideServiceImpl implements LessonSlideService {

  private static final Pattern NON_ALNUM = Pattern.compile("[^a-zA-Z0-9._-]");
  private static final String PPTX_MIME =
      "application/vnd.openxmlformats-officedocument.presentationml.presentation";

  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;
  SlideTemplateRepository slideTemplateRepository;
  MinioClient minioClient;
  MinioProperties minioProperties;

  @Override
  @Transactional(readOnly = true)
  public LessonSlideGeneratedContentResponse generateLessonContentDraft(
      LessonSlideGenerateContentRequest request) {
    validateTeacherRole();

    boolean gradeExists = schoolGradeRepository.existsByGradeLevelAndIsActiveTrue(request.getGradeLevel());
    if (!gradeExists) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND);
    }

    Subject subject =
        subjectRepository.findById(request.getSubjectId()).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    if (Boolean.FALSE.equals(subject.getIsActive()) || subject.getDeletedAt() != null) {
      throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
    }

    if (subject.getSchoolGrade() == null
        || !subject.getSchoolGrade().getGradeLevel().equals(request.getGradeLevel())
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
    DeckSections deckSections = buildDeckSections(lesson, request.getAdditionalPrompt());
    List<LessonSlideJsonItemResponse> slides = buildPreviewSlides(deckSections, slideCount);

    return LessonSlideGeneratedContentResponse.builder()
        .subjectId(subject.getId())
        .chapterId(chapter.getId())
        .lessonId(lesson.getId())
        .lessonTitle(lesson.getTitle())
        .slideCount(slideCount)
        .slides(slides)
        .additionalPrompt(request.getAdditionalPrompt())
        .build();
  }

  @Override
  @Transactional
  public LessonResponse confirmLessonContent(UUID lessonId, LessonSlideConfirmContentRequest request) {
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
  @Transactional
  public SlideTemplateResponse uploadTemplate(String name, String description, MultipartFile file) {
    validateTeacherRole();
    validatePptxFile(file);
    ensureTemplateBucketExists();

    UUID userId = SecurityUtils.getCurrentUserId();
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
            .bucketName(minioProperties.getTemplateBucket())
            .uploadedBy(userId)
            .isActive(true)
            .build();

    return toTemplateResponse(slideTemplateRepository.save(template));
  }

  @Override
  @Transactional
  public SlideTemplateResponse updateTemplate(
      UUID templateId, String name, String description, Boolean active, MultipartFile file) {
    validateTeacherRole();

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
        for (XSLFShape shape : slide.getShapes()) {
          if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text == null || text.isBlank()) {
              continue;
            }
            String replaced = replacePlaceholders(text, slideValues);
            if (!text.equals(replaced)) {
              textShape.clearText();
              textShape.setText(replaced);
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
    return replaced;
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
        for (XSLFShape shape : slide.getShapes()) {
          if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text == null || text.isBlank()) {
              continue;
            }
            String replaced = replacePlaceholders(text, slideValues);
            if (!text.equals(replaced)) {
              textShape.clearText();
              textShape.setText(replaced);
            }
          }
        }
      }

      slideshow.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception ex) {
      log.error("Failed to generate pptx from confirmed json", ex);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

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

    slides.add(buildPreviewSlide(normalizedCount - 1, "SUMMARY", "Tong ket", deck.closingSummary()));
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

  private String buildObjectKey(String originalFileName) {
    String normalized =
        originalFileName == null
            ? "template.pptx"
            : Normalizer.normalize(originalFileName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    String sanitized = NON_ALNUM.matcher(normalized).replaceAll("_");
    return "slide-templates/" + UUID.randomUUID() + "/" + System.currentTimeMillis() + "-" + sanitized;
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
        .active(Boolean.TRUE.equals(template.getIsActive()))
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .build();
  }

  private void validateTeacherRole() {
    if (!SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }
}
