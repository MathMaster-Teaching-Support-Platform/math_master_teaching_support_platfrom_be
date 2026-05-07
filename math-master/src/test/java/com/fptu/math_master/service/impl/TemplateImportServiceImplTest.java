package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.TemplateImportResponse;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GeminiService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("TemplateImportServiceImpl - Tests")
class TemplateImportServiceImplTest extends BaseUnitTest {

  @InjectMocks private TemplateImportServiceImpl templateImportService;

  @Mock private GeminiService geminiService;
  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private QuestionBankRepository questionBankRepository;
  @Mock private UserRepository userRepository;
  @Mock private MultipartFile multipartFile;

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = TemplateImportServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(templateImportService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  private void mockJwtAuth(UUID userId) {
    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", userId.toString(), "scope", "ROLE_TEACHER"));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("validateFile()")
  class ValidateFileTests {

    /**
     * Normal case: Accept plain text file under size limit.
     *
     * <p>Input:
     * <ul>
     *   <li>file: non-empty, size=2048, contentType=text/plain</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>file present and not empty branch</li>
     *   <li>size within max branch</li>
     *   <li>allowed content type branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return true</li>
     * </ul>
     */
    @Test
    void it_should_return_true_when_file_is_valid_text_file() {
      // ===== ARRANGE =====
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(2048L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("de-toan-12.txt");

      // ===== ACT =====
      boolean valid = templateImportService.validateFile(multipartFile);

      // ===== ASSERT =====
      assertTrue(valid);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    /**
     * Abnormal case: Reject file when content type is not allowed.
     *
     * <p>Input:
     * <ul>
     *   <li>file: non-empty, valid size, contentType=image/png</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>invalid content type TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return false</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_file_content_type_is_not_supported() {
      // ===== ARRANGE =====
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(2048L);
      when(multipartFile.getContentType()).thenReturn("image/png");
      when(multipartFile.getOriginalFilename()).thenReturn("hinh-anh.png");

      // ===== ACT =====
      boolean valid = templateImportService.validateFile(multipartFile);

      // ===== ASSERT =====
      assertFalse(valid);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_false_when_file_size_exceeds_max_limit() {
      // ===== ARRANGE =====
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(11L * 1024 * 1024);
      when(multipartFile.getContentType()).thenReturn("text/plain");

      // ===== ACT =====
      boolean valid = templateImportService.validateFile(multipartFile);

      // ===== ASSERT =====
      assertFalse(valid);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    /**
     * Resolves content type from filename when {@code getContentType()} is null and extension is
     * allowed.
     */
    @Test
    void it_should_return_true_when_content_type_is_null_but_filename_allows_guess() {
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(100L);
      when(multipartFile.getContentType()).thenReturn(null);
      when(multipartFile.getOriginalFilename()).thenReturn("import-me.txt");

      assertTrue(templateImportService.validateFile(multipartFile));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_accept_legacy_msword_when_filename_ends_with_doc() {
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(100L);
      when(multipartFile.getContentType()).thenReturn(null);
      when(multipartFile.getOriginalFilename()).thenReturn("template.doc");

      assertTrue(templateImportService.validateFile(multipartFile));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("extractJSON()")
  class ExtractJsonTests {

    /**
     * Normal case: Extract JSON payload from mixed AI text response.
     *
     * <p>Input:
     * <ul>
     *   <li>content: wrapper text + JSON object + trailing text</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>finalStart/finalEnd extraction branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return isolated JSON string only</li>
     * </ul>
     */
    @Test
    void it_should_extract_json_segment_when_response_contains_wrapper_text() {
      // ===== ARRANGE =====
      String content = "AI analysis result:\n{\"analysisSuccessful\":true,\"confidenceScore\":0.9}\nend";

      // ===== ACT =====
      String json = invokePrivate("extractJSON", new Class<?>[] {String.class}, content);

      // ===== ASSERT =====
      assertEquals("{\"analysisSuccessful\":true,\"confidenceScore\":0.9}", json);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("detectLanguage()")
  class DetectLanguageTests {

    /**
     * Normal case: Detect Vietnamese from accented characters.
     *
     * <p>Input:
     * <ul>
     *   <li>text: Vietnamese sentence with diacritics</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>Vietnamese character regex TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return {@code vi}</li>
     * </ul>
     */
    @Test
    void it_should_return_vi_when_text_contains_vietnamese_diacritics() {
      // ===== ARRANGE =====
      String vietnameseText = "Giải phương trình bậc nhất với hệ số thực.";

      // ===== ACT =====
      String lang = invokePrivate("detectLanguage", new Class<?>[] {String.class}, vietnameseText);

      // ===== ASSERT =====
      assertEquals("vi", lang);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    /**
     * Normal case: Return English when text has no Vietnamese markers.
     *
     * <p>Input:
     * <ul>
     *   <li>text: plain English math prompt</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>Vietnamese regex FALSE branch</li>
     *   <li>Vietnamese keyword FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return {@code en}</li>
     * </ul>
     */
    @Test
    void it_should_return_en_when_text_has_no_vietnamese_indicators() {
      // ===== ARRANGE =====
      String englishText = "Solve the linear equation with integer coefficients.";

      // ===== ACT =====
      String lang = invokePrivate("detectLanguage", new Class<?>[] {String.class}, englishText);

      // ===== ASSERT =====
      assertEquals("en", lang);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("guessContentType()")
  class GuessContentTypeTests {

    /**
     * Normal case: Detect DOCX content type by file extension.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return DOCX mime type</li>
     * </ul>
     */
    @Test
    void it_should_return_docx_mime_type_when_filename_has_docx_extension() {
      // ===== ARRANGE =====
      String filename = "de-thi-hoc-ky.docx";

      // ===== ACT =====
      String mime = invokePrivate("guessContentType", new Class<?>[] {String.class}, filename);

      // ===== ASSERT =====
      assertEquals(
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", mime);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_null_when_filename_extension_is_unknown() {
      // ===== ARRANGE =====
      String filename = "archive.bin";

      // ===== ACT =====
      String mime = invokePrivate("guessContentType", new Class<?>[] {String.class}, filename);

      // ===== ASSERT =====
      assertEquals(null, mime);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_null_when_filename_is_null() {
      assertNull(invokePrivate("guessContentType", new Class<?>[] {String.class}, (String) null));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("detectQuestionType()")
  class DetectQuestionTypeTests {

    @Test
    void it_should_return_true_false_when_text_contains_true_or_false_marker() {
      // ===== ARRANGE =====
      String text = "Decide true or false for each statement.";

      // ===== ACT =====
      QuestionType type = invokePrivate("detectQuestionType", new Class<?>[] {String.class}, text);

      // ===== ASSERT =====
      assertEquals(QuestionType.TRUE_FALSE, type);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_multiple_choice_when_text_contains_option_pattern() {
      // ===== ARRANGE =====
      String text = "A) 2  B) 3  C) 4  D) 5";

      // ===== ACT =====
      QuestionType type = invokePrivate("detectQuestionType", new Class<?>[] {String.class}, text);

      // ===== ASSERT =====
      assertEquals(QuestionType.MULTIPLE_CHOICE, type);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("parsers and builders")
  class ParserAndBuilderTests {

    @Test
    void it_should_fallback_to_default_question_type_when_type_string_is_invalid() {
      // ===== ARRANGE =====
      String invalid = "RANDOM_TYPE";

      // ===== ACT =====
      QuestionType type = invokePrivate("parseQuestionType", new Class<?>[] {String.class}, invalid);

      // ===== ASSERT =====
      assertEquals(QuestionType.MULTIPLE_CHOICE, type);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_fallback_to_apply_cognitive_level_when_level_string_is_invalid() {
      // ===== ARRANGE =====
      String invalid = "UNKNOWN_LEVEL";

      // ===== ACT =====
      CognitiveLevel level =
          invokePrivate("parseCognitiveLevel", new Class<?>[] {String.class}, invalid);

      // ===== ASSERT =====
      assertEquals(CognitiveLevel.APPLY, level);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_parse_json_array_to_string_list_when_node_is_array() throws Exception {
      // ===== ARRANGE =====
      JsonNode node = mapper.readTree("[\"algebra\",\"geometry\"]");

      // ===== ACT =====
      List<String> values = invokePrivate("parseStringArray", new Class<?>[] {JsonNode.class}, node);

      // ===== ASSERT =====
      assertEquals(2, values.size());
      assertEquals("algebra", values.get(0));

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_empty_list_when_parse_string_array_node_is_not_array() throws Exception {
      JsonNode node = mapper.readTree("\"not-an-array\"");
      @SuppressWarnings("unchecked")
      List<String> values = invokePrivate("parseStringArray", new Class<?>[] {JsonNode.class}, node);
      assertTrue(values.isEmpty());
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_build_empty_params_when_parameters_node_is_not_array() throws Exception {
      JsonNode node = mapper.readTree("{\"a\":1}");
      @SuppressWarnings("unchecked")
      Map<String, Object> map =
          invokePrivate("buildParametersFromNode", new Class<?>[] {JsonNode.class}, node);
      assertTrue(map.isEmpty());
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_build_parameter_map_when_parameters_node_contains_items() throws Exception {
      // ===== ARRANGE =====
      JsonNode node = mapper.readTree("[{\"name\":\"a\",\"type\":\"integer\",\"min\":1,\"max\":9}]");

      // ===== ACT =====
      Map<String, Object> params =
          invokePrivate("buildParametersFromNode", new Class<?>[] {JsonNode.class}, node);

      // ===== ASSERT =====
      assertTrue(params.containsKey("a"));
      assertNotNull(params.get("a"));

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_add_default_tag_when_all_input_tags_are_invalid() {
      // ===== ARRANGE =====
      List<String> tags = List.of("unknown_tag_1", "unknown_tag_2");

      // ===== ACT =====
      List<?> parsed =
          invokePrivate("parseTagsFromStringArray", new Class<?>[] {List.class}, tags);

      // ===== ASSERT =====
      assertEquals(1, parsed.size());

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_resolve_tag_from_vietnamese_name_when_not_valid_enum() {
      List<String> tags = List.of("Tam giác");
      @SuppressWarnings("unchecked")
      List<com.fptu.math_master.enums.QuestionTag> parsed =
          invokePrivate("parseTagsFromStringArray", new Class<?>[] {List.class}, tags);
      assertEquals(1, parsed.size());
      assertEquals(
          com.fptu.math_master.enums.QuestionTag.TRIANGLES, parsed.get(0));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("performRuleBasedAnalysis()")
  class PerformRuleBasedAnalysisTests {

    /**
     * Normal case: Build fallback template when AI is unavailable.
     *
     * <p>Input:
     * <ul>
     *   <li>text: includes numbers and option markers</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>numbers detected branch TRUE</li>
     *   <li>question type detection branch for MULTIPLE_CHOICE</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>analysisSuccessful is false with fallback warnings</li>
     * </ul>
     */
    @Test
    void it_should_return_fallback_analysis_when_rule_based_analysis_is_invoked() {
      // ===== ARRANGE =====
      String text = "Question 1: A) 1 B) 2 C) 3 D) 4. Solve 2x + 5 = 11.";

      // ===== ACT =====
      TemplateImportResponse response =
          invokePrivate("performRuleBasedAnalysis", new Class<?>[] {String.class}, text);

      // ===== ASSERT =====
      assertFalse(response.getAnalysisSuccessful());
      assertNotNull(response.getSuggestedTemplate());
      assertEquals(QuestionType.MULTIPLE_CHOICE, response.getAnalysis().getDetectedType());
      assertTrue(response.getWarnings().size() >= 1);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("importTemplateFromFile()")
  class ImportTemplateFromFileTests {

    @Test
    void it_should_return_error_response_when_file_is_invalid() {
      // ===== ARRANGE =====
      when(multipartFile.isEmpty()).thenReturn(true);

      // ===== ACT =====
      TemplateImportResponse response =
          templateImportService.importTemplateFromFile(multipartFile, "Đại số", "Lớp 10", null, null);

      // ===== ASSERT =====
      assertFalse(response.getAnalysisSuccessful());
      assertTrue(response.getWarnings().contains("Invalid file format or size"));

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_return_fallback_response_when_ai_generation_fails() throws IOException {
      // ===== ARRANGE =====
      UUID currentUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
      mockJwtAuth(currentUserId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(1024L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("toan-lop-10.txt");
      when(multipartFile.getBytes())
          .thenReturn("Câu 1: Giải phương trình 2x + 3 = 7".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      when(geminiService.sendMessage(org.mockito.ArgumentMatchers.anyString()))
          .thenThrow(new RuntimeException("AI service timeout"));
      when(questionTemplateRepository.save(org.mockito.ArgumentMatchers.any(QuestionTemplate.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      TemplateImportResponse response =
          templateImportService.importTemplateFromFile(multipartFile, "Đại số", "Học kỳ 1", null, null);

      // ===== ASSERT =====
      assertNotNull(response);
      assertNotNull(response.getExtractedText());
      assertFalse(response.getAnalysisSuccessful());
      assertNotNull(response.getSuggestedTemplate());

      // ===== VERIFY =====
      verifyNoInteractions(questionBankRepository);
    }

    @Test
    void it_should_extract_plain_text_when_using_extract_text_method() throws IOException {
      // ===== ARRANGE =====
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("input.txt");
      when(multipartFile.getBytes()).thenReturn("Bài toán đại số".getBytes(StandardCharsets.UTF_8));

      // ===== ACT =====
      String text = templateImportService.extractTextFromFile(multipartFile);

      // ===== ASSERT =====
      assertEquals("Bài toán đại số", text);
    }

    @Test
    void it_should_throw_runtime_exception_when_extracting_unsupported_content_type() {
      // ===== ARRANGE =====
      when(multipartFile.getContentType()).thenReturn("application/zip");
      when(multipartFile.getOriginalFilename()).thenReturn("archive.zip");

      // ===== ACT & ASSERT =====
      IllegalArgumentException exception =
          org.junit.jupiter.api.Assertions.assertThrows(
              IllegalArgumentException.class, () -> templateImportService.extractTextFromFile(multipartFile));
      assertTrue(exception.getMessage().contains("Unsupported file type"));
    }
  }

  @Nested
  @DisplayName("AI parsing and post-processing")
  class AiParsingAndPostProcessingTests {

    @Test
    void it_should_include_subject_and_context_when_building_analysis_prompt() {
      // ===== ARRANGE =====
      String text = "Giải phương trình bậc nhất.";
      String subject = "Đại số";
      String context = "Lớp 10";

      // ===== ACT =====
      String prompt =
          invokePrivate(
              "buildAnalysisPrompt",
              new Class<?>[] {String.class, String.class, String.class},
              text,
              subject,
              context);

      // ===== ASSERT =====
      assertTrue(prompt.contains("Subject Area: Đại số"));
      assertTrue(prompt.contains("Additional Context: Lớp 10"));
      assertTrue(prompt.contains(text));

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_omit_optional_context_lines_when_subject_and_context_are_blank() {
      String text = "Compute 1+1.";
      String prompt =
          invokePrivate(
              "buildAnalysisPrompt",
              new Class<?>[] {String.class, String.class, String.class},
              text,
              "",
              null);
      assertTrue(prompt.contains(text));
      assertFalse(prompt.contains("Subject Area:"));
      assertFalse(prompt.contains("Additional Context:"));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_include_only_subject_line_when_context_is_empty() {
      String prompt =
          invokePrivate(
              "buildAnalysisPrompt",
              new Class<?>[] {String.class, String.class, String.class},
              "body",
              "Hình học",
              "");
      assertTrue(prompt.contains("Subject Area: Hình học"));
      assertFalse(prompt.contains("Additional Context:"));
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_parse_ai_analysis_json_when_response_structure_is_valid() {
      // ===== ARRANGE =====
      String aiJson =
          """
          {
            "analysisSuccessful": true,
            "confidenceScore": 0.88,
            "extractedText": "Giải phương trình: 2x + 3 = 7",
            "analysis": {
              "detectedType": "MULTIPLE_CHOICE",
              "detectedPatterns": ["Linear equation pattern: ax + b = c"],
              "detectedFormulas": ["2x + 3 = 7"],
              "mathematicalStructure": "Linear equation",
              "detectedLanguage": "vi"
            },
            "suggestedTemplate": {
              "name": "Linear Equation Template",
              "description": "Template for equation solving",
              "templateType": "MULTIPLE_CHOICE",
              "templateText": "Giải phương trình: {{a}}x + {{b}} = {{c}}",
              "parameters": [
                {"name":"a","type":"integer","min":1,"max":10,"description":"coefficient"},
                {"name":"b","type":"integer","min":-20,"max":20,"description":"constant"},
                {"name":"c","type":"integer","min":1,"max":50,"description":"result"}
              ],
              "answerFormula": "(c - b) / a",
              "cognitiveLevel": "APPLY",
              "tags": ["problem_solving"]
            },
            "warnings": ["check constraints"]
          }
          """;

      // ===== ACT =====
      TemplateImportResponse response =
          invokePrivate(
              "parseAIAnalysis",
              new Class<?>[] {String.class, String.class},
              aiJson,
              "Giải phương trình: 2x + 3 = 7");

      // ===== ASSERT =====
      assertTrue(response.getAnalysisSuccessful());
      assertEquals(QuestionType.MULTIPLE_CHOICE, response.getAnalysis().getDetectedType());
      assertNotNull(response.getSuggestedTemplate());
      assertNotNull(response.getSuggestedTemplate().getTemplateText());
      assertTrue(response.getWarnings().size() >= 1);

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_fallback_when_ai_analysis_json_is_invalid() {
      // ===== ARRANGE =====
      String invalidJson = "not-a-valid-json-payload";

      // ===== ACT =====
      TemplateImportResponse response =
          invokePrivate(
              "parseAIAnalysis",
              new Class<?>[] {String.class, String.class},
              invalidJson,
              "Giải phương trình");

      // ===== ASSERT =====
      assertFalse(response.getAnalysisSuccessful());
      assertNotNull(response.getSuggestedTemplate());

      // ===== VERIFY =====
      verifyNoInteractions(geminiService, questionTemplateRepository, questionBankRepository);
    }

    @Test
    void it_should_add_warning_for_division_by_zero_constraint_when_formula_uses_division_by_a() {
      // ===== ARRANGE =====
      TemplateImportResponse response =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(new java.util.ArrayList<>())
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("en")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.SHORT_ANSWER)
                      .templateText(Map.of("en", "Solve {{a}}x + {{b}} = {{c}}"))
                      .answerFormula("(c - b) / a")
                      .build())
              .build();

      // ===== ACT =====
      TemplateImportResponse processed =
          invokePrivate(
              "postProcessAIResponse",
              new Class<?>[] {TemplateImportResponse.class, String.class},
              response,
              "Solve equation");

      // ===== ASSERT =====
      assertTrue(
          processed.getWarnings().stream().anyMatch(w -> w.contains("a != 0") || w.contains("division by zero")));
    }

    @Test
    void it_should_skip_language_auto_fill_when_response_has_no_analysis_object() {
      TemplateImportResponse in =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(new java.util.ArrayList<>())
              .analysis(null)
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.SHORT_ANSWER)
                      .templateText(Map.of("en", "Q"))
                      .build())
              .build();
      TemplateImportResponse out =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              in,
              "plain");
      assertNotNull(out.getSuggestedTemplate());
    }

    @Test
    void it_should_skip_duplicate_division_warning_when_a_constraint_already_mentioned() {
      java.util.ArrayList<String> w = new java.util.ArrayList<>();
      w.add("User note: a != 0 is required");
      TemplateImportResponse response =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(w)
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("en")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.SHORT_ANSWER)
                      .templateText(Map.of("en", "{{a}}x"))
                      .answerFormula("b / a")
                      .build())
              .build();
      TemplateImportResponse processed =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              response,
              "t");
      long critical =
          processed.getWarnings().stream().filter(s -> s.contains("CRITICAL") && s.contains("a !="))
              .count();
      assertEquals(0, critical);
    }

    @Test
    void it_should_return_rule_based_result_when_json_reports_analysis_unsuccessful() {
      String json =
          """
          {"analysisSuccessful": false, "suggestedTemplate": {"name": "X"}}
          """;
      TemplateImportResponse r =
          invokePrivate("parseAIAnalysis", new Class<?>[] {String.class, String.class}, json, "x");
      assertFalse(r.getAnalysisSuccessful());
    }

    @Test
    void it_should_return_rule_based_result_when_analysis_node_is_missing() {
      String json =
          """
          {"analysisSuccessful": true, "suggestedTemplate": {"name": "T", "templateType": "MULTIPLE_CHOICE"}}
          """;
      TemplateImportResponse r =
          invokePrivate("parseAIAnalysis", new Class<?>[] {String.class, String.class}, json, "x");
      assertFalse(r.getAnalysisSuccessful());
    }

    @Test
    void it_should_use_multilingual_map_when_template_text_is_json_object() {
      String json =
          """
          {
            "analysisSuccessful": true,
            "confidenceScore": 0.9,
            "analysis": {
              "detectedType": "SHORT_ANSWER",
              "detectedPatterns": [],
              "detectedFormulas": [],
              "mathematicalStructure": "t",
              "detectedLanguage": "en"
            },
            "suggestedTemplate": {
              "name": "T",
              "description": "d",
              "templateType": "SHORT_ANSWER",
              "templateText": {"en": "e", "vi": "v"},
              "parameters": [],
              "answerFormula": "",
              "cognitiveLevel": "REMEMBER",
              "tags": ["PROBLEM_SOLVING"]
            },
            "warnings": []
          }
          """;
      TemplateImportResponse r =
          invokePrivate(
              "parseAIAnalysis", new Class<?>[] {String.class, String.class}, json, "e\nv");
      assertTrue(r.getAnalysisSuccessful());
      assertEquals(2, r.getSuggestedTemplate().getTemplateText().size());
    }

    @Test
    void it_should_parse_suggested_template_when_parameters_key_is_absent() {
      String json =
          """
          {
            "analysisSuccessful": true,
            "analysis": {
              "detectedType": "SHORT_ANSWER",
              "mathematicalStructure": "m",
              "detectedLanguage": "en"
            },
            "suggestedTemplate": {
              "name": "NoParams",
              "templateType": "SHORT_ANSWER",
              "templateText": "Plain string text",
              "answerFormula": ""
            }
          }
          """;
      TemplateImportResponse r =
          invokePrivate("parseAIAnalysis", new Class<?>[] {String.class, String.class}, json, "q");
      assertTrue(r.getAnalysisSuccessful());
      assertNotNull(r.getAnalysis().getPlaceholderSuggestions());
    }

    @Test
    void it_should_default_empty_english_key_when_template_text_is_neither_string_nor_object() {
      String json =
          """
          {
            "analysisSuccessful": true,
            "analysis": {
              "detectedType": "SHORT_ANSWER",
              "mathematicalStructure": "m",
              "detectedLanguage": "en"
            },
            "suggestedTemplate": {
              "name": "N",
              "templateType": "SHORT_ANSWER",
              "templateText": 42,
              "parameters": []
            }
          }
          """;
      TemplateImportResponse r =
          invokePrivate("parseAIAnalysis", new Class<?>[] {String.class, String.class}, json, "q");
      assertTrue(r.getAnalysisSuccessful());
      assertEquals("", r.getSuggestedTemplate().getTemplateText().get("en"));
    }

    @Test
    void it_should_post_process_vi_key_when_english_stored_with_detected_vi() {
      TemplateImportResponse in =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(new java.util.ArrayList<>())
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("vi")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.SHORT_ANSWER)
                      .templateText(new java.util.HashMap<>(Map.of("en", "Nội dung câu")))
                      .build())
              .build();
      TemplateImportResponse out =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              in,
              "câu hỏi");
      assertTrue(out.getWarnings().stream().anyMatch(w -> w.contains("en' to 'vi'")));
    }

    @Test
    void it_should_set_default_mcq_options_only_when_both_mode_and_field_empty() {
      TemplateImportResponse in =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(new java.util.ArrayList<>())
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("en")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.MULTIPLE_CHOICE)
                      .templateText(Map.of("en", "Pick one"))
                      .optionsGenerator(new java.util.HashMap<>())
                      .build())
              .build();
      TemplateImportResponse out =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              in,
              "Q");
      assertNotNull(out.getSuggestedTemplate().getOptionsGenerator().get("distractors"));
    }

    @Test
    void it_should_not_replace_mcq_options_when_generator_map_is_not_empty() {
      Map<String, Object> gen = new java.util.HashMap<>();
      gen.put("count", 4);
      gen.put("distractors", List.of("a"));
      TemplateImportResponse in =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .warnings(new java.util.ArrayList<>())
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("en")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.MULTIPLE_CHOICE)
                      .templateText(Map.of("en", "Q"))
                      .optionsGenerator(gen)
                      .build())
              .build();
      TemplateImportResponse out =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              in,
              "Q");
      assertEquals(1, ((List<?>) out.getSuggestedTemplate().getOptionsGenerator().get("distractors")).size());
    }

    @Test
    void it_should_return_same_response_when_post_process_fails_on_null_warnings() {
      TemplateImportResponse in =
          TemplateImportResponse.builder()
              .analysisSuccessful(true)
              .analysis(
                  TemplateImportResponse.QuestionStructureAnalysis.builder()
                      .detectedLanguage("en")
                      .build())
              .suggestedTemplate(
                  TemplateImportResponse.TemplateDraft.builder()
                      .templateType(QuestionType.SHORT_ANSWER)
                      .templateText(Map.of("en", "a"))
                      .build())
              .build();
      TemplateImportResponse out =
          invokePrivate(
              "postProcessAIResponse", new Class<?>[] {TemplateImportResponse.class, String.class},
              in,
              "x");
      assertEquals(in, out);
    }

    @Test
    void it_should_inject_sample_question_lines_for_question_marks() throws Exception {
      String text = "1) First line with question?\n2) Second: regular";
      @SuppressWarnings("unchecked")
      List<String> lines =
          invokePrivate("extractSampleQuestions", new Class<?>[] {String.class}, text);
      assertFalse(lines.isEmpty());
    }
  }

  @Nested
  @DisplayName("extractJSON() additional branches")
  class ExtractJsonAdditionalTests {

    @Test
    void it_should_return_trimmed_content_when_no_json_brackets_found() {
      String out = invokePrivate("extractJSON", new Class<?>[] {String.class}, "  plain text  ");
      assertEquals("plain text", out);
    }

    @Test
    void it_should_return_null_when_extract_json_input_is_null() {
      assertNull(invokePrivate("extractJSON", new Class<?>[] {String.class}, (String) null));
    }

    @Test
    void it_should_prefer_earlier_bracket_for_object_and_later_closing_bracket() {
      String content = "preamble {\"a\":1} trailer";
      String json = invokePrivate("extractJSON", new Class<?>[] {String.class}, content);
      assertTrue(json.contains("\"a\":1"));
    }

    @Test
    void it_should_handle_json_array_brackets() {
      String json = invokePrivate("extractJSON", new Class<?>[] {String.class}, "x [1,2,3] y");
      assertTrue(json.startsWith("["));
    }
  }

  @Nested
  @DisplayName("helper methods — numbers, language, type")
  class HelperBranchTests {

    @Test
    void it_should_return_en_for_detect_language_with_null() {
      assertEquals("en", invokePrivate("detectLanguage", new Class<?>[] {String.class}, (Object) null));
    }

    @Test
    void it_should_return_en_for_empty_detect_language_string() {
      assertEquals("en", invokePrivate("detectLanguage", new Class<?>[] {String.class}, ""));
    }

    @Test
    void it_should_detect_vi_from_keyword_without_diacritics_cau() {
      String lang = invokePrivate("detectLanguage", new Class<?>[] {String.class}, "làm câu 1 số 3");
      assertEquals("vi", lang);
    }

    @Test
    void it_should_return_true_false_type_for_vietnamese_marker() {
      assertEquals(
          QuestionType.TRUE_FALSE,
          invokePrivate("detectQuestionType", new Class<?>[] {String.class}, "Chọn đúng hay sai"));
    }

    @Test
    void it_should_list_numbers_in_extract_numbers() {
      @SuppressWarnings("unchecked")
      List<String> numbers =
          invokePrivate("extractNumbers", new Class<?>[] {String.class}, "A 3.5 and 10 and x");
      assertTrue(numbers.size() >= 2);
    }

    @Test
    void it_should_perform_rule_based_without_number_placeholders_when_text_has_no_digits() {
      TemplateImportResponse r =
          invokePrivate(
              "performRuleBasedAnalysis", new Class<?>[] {String.class}, "No digits at all, only text.");
      assertNotNull(r.getAnalysis().getPlaceholderSuggestions());
    }

    @Test
    void it_should_parse_placeholders_array_via_private_parsePlaceholders() throws Exception {
      JsonNode node =
          mapper.readTree(
              "[{\"name\":\"a\",\"type\":\"int\",\"min\":0,\"max\":1,\"description\":\"d\",\"examples\":[\"1\"]}]");
      @SuppressWarnings("unchecked")
      List<TemplateImportResponse.PlaceholderSuggestion> list =
          invokePrivate("parsePlaceholders", new Class<?>[] {JsonNode.class}, node);
      assertEquals(1, list.size());
      assertEquals("a", list.get(0).getVariableName());
    }
  }

  @Nested
  @DisplayName("extractTextFromFile() — real PDF/DOCX and IO")
  class ExtractTextFileIntegrationTests {

    @Test
    void it_should_extract_text_from_pdf_bytes() throws Exception {
      byte[] pdfBytes;
      try (PDDocument doc = new PDDocument()) {
        doc.addPage(new PDPage());
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          doc.save(os);
          pdfBytes = os.toByteArray();
        }
      }
      MockMultipartFile f =
          new MockMultipartFile("f", "doc.pdf", "application/pdf", pdfBytes);
      String text = templateImportService.extractTextFromFile(f);
      assertNotNull(text);
    }

    @Test
    void it_should_extract_paragraphs_from_docx_bytes() throws Exception {
      byte[] docxBytes;
      try (XWPFDocument doc = new XWPFDocument();
          ByteArrayOutputStream os = new ByteArrayOutputStream()) {
        doc.createParagraph().createRun().setText("Câu 1 từ DOCX 42");
        doc.write(os);
        docxBytes = os.toByteArray();
      }
      MockMultipartFile f =
          new MockMultipartFile(
              "f",
              "a.docx",
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
              docxBytes);
      String text = templateImportService.extractTextFromFile(f);
      assertTrue(text.contains("42"));
    }

    @Test
    void it_should_guess_pdf_from_filename_when_content_type_is_null() throws Exception {
      byte[] pdfBytes;
      try (PDDocument doc = new PDDocument()) {
        doc.addPage(new PDPage());
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          doc.save(os);
          pdfBytes = os.toByteArray();
        }
      }
      MockMultipartFile f = new MockMultipartFile("f", "only.pdf", null, pdfBytes);
      String text = templateImportService.extractTextFromFile(f);
      assertNotNull(text);
    }

    @Test
    void it_should_wrap_ioexception_in_runtime_exception() throws Exception {
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("a.txt");
      when(multipartFile.getBytes()).thenThrow(new IOException("disk read error"));

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> templateImportService.extractTextFromFile(multipartFile));
      assertTrue(ex.getMessage().contains("Failed to extract text"));
    }

    @Test
    void it_should_fail_when_cannot_guess_mime() {
      MockMultipartFile f = new MockMultipartFile("f", null, null, new byte[] {1, 2, 3});
      assertThrows(IllegalArgumentException.class, () -> templateImportService.extractTextFromFile(f));
    }
  }

  @Nested
  @DisplayName("importTemplateFromFile() — success and edge branches")
  class ImportFileDeepBranchTests {

    private String validAiResponse() {
      return
          """
          {
            "analysisSuccessful": true,
            "confidenceScore": 0.88,
            "extractedText": "1) A) 1",
            "analysis": {
              "detectedType": "MULTIPLE_CHOICE",
              "detectedPatterns": ["p"],
              "detectedFormulas": ["1"],
              "mathematicalStructure": "m",
              "detectedLanguage": "en"
            },
            "suggestedTemplate": {
              "name": "Imported from UT",
              "description": "d",
              "templateType": "MULTIPLE_CHOICE",
              "templateText": "Pick {{a}} or {{b}}",
              "parameters": [],
              "answerFormula": "a + b",
              "cognitiveLevel": "APPLY",
              "tags": ["PROBLEM_SOLVING"]
            },
            "warnings": []
          }
          """;
    }

    @Test
    void it_should_save_draft_template_when_analysis_succeeds() throws Exception {
      UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(200L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("f.txt");
      when(multipartFile.getBytes())
          .thenReturn("1) A) 1  B) 2".getBytes(StandardCharsets.UTF_8));
      when(geminiService.sendMessage(anyString())).thenReturn(validAiResponse());
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenAnswer(
              inv -> {
                QuestionTemplate t = inv.getArgument(0);
                return t;
              });

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, "Algebra", "U1", null, null);

      assertTrue(r.getAnalysisSuccessful());
      verify(questionTemplateRepository, times(1)).save(any(QuestionTemplate.class));
    }

    @Test
    void it_should_add_warning_when_saving_draft_fails() throws Exception {
      UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(200L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("f.txt");
      when(multipartFile.getBytes())
          .thenReturn("1) A) 1".getBytes(StandardCharsets.UTF_8));
      when(geminiService.sendMessage(anyString())).thenReturn(validAiResponse());
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenThrow(new RuntimeException("db failure"));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, null, null);

      assertTrue(r.getAnalysisSuccessful());
      assertTrue(
          r.getWarnings().stream()
              .anyMatch(w -> w.toLowerCase().contains("auto-save") || w.contains("DRAFT")));
    }

    @Test
    void it_should_error_when_file_has_only_whitespace_text() throws Exception {
      UUID userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(20L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("e.txt");
      when(multipartFile.getBytes())
          .thenReturn("  \n\t  ".getBytes(StandardCharsets.UTF_8));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, null, null);
      assertFalse(r.getAnalysisSuccessful());
      assertTrue(
          r.getWarnings().stream()
              .anyMatch(w -> w.toLowerCase().contains("no text") || w.contains("content")));
    }

    @Test
    void it_should_error_when_get_bytes_fails() throws Exception {
      UUID userId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(20L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getBytes()).thenThrow(new IOException("io"));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, null, null);
      assertFalse(r.getAnalysisSuccessful());
      assertTrue(
          r.getWarnings().get(0).toLowerCase().contains("process")
          || r.getWarnings().get(0).toLowerCase().contains("io"));
    }

    @Test
    void it_should_error_when_question_bank_id_unknown() throws Exception {
      UUID userId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(30L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("a.txt");
      when(multipartFile.getBytes()).thenReturn("1 and 2".getBytes(StandardCharsets.UTF_8));
      UUID bankId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.empty());

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, bankId, null);
      assertFalse(r.getAnalysisSuccessful());
    }

    @Test
    void it_should_error_when_user_cannot_access_private_question_bank() throws Exception {
      UUID me = UUID.fromString("10101010-1010-1010-1010-101010101010");
      mockJwtAuth(me);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(30L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getBytes()).thenReturn("12 + 3".getBytes(StandardCharsets.UTF_8));
      UUID bankId = UUID.fromString("20202020-2020-2020-2020-202020202020");
      QuestionBank bank = new QuestionBank();
      bank.setId(bankId);
      bank.setTeacherId(UUID.fromString("30303030-3030-3030-3030-303030303030"));
      bank.setName("B");
      bank.setIsPublic(false);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, bankId, null);
      assertFalse(r.getAnalysisSuccessful());
      assertTrue(
          r.getWarnings().get(0).toLowerCase().contains("process")
          || r.getWarnings().get(0).toLowerCase().contains("access"));
    }

    @Test
    void it_should_allow_import_when_public_question_bank_belongs_to_another_teacher() throws Exception {
      UUID me = UUID.fromString("40404040-4040-4040-4040-404040404040");
      mockJwtAuth(me);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(200L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("a.txt");
      when(multipartFile.getBytes())
          .thenReturn("1) A) 1".getBytes(StandardCharsets.UTF_8));
      UUID bankId = UUID.fromString("50505050-5050-5050-5050-505050505050");
      QuestionBank bank = new QuestionBank();
      bank.setId(bankId);
      bank.setTeacherId(UUID.fromString("60606060-6060-6060-6060-606060606060"));
      bank.setName("pub");
      bank.setIsPublic(true);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(geminiService.sendMessage(anyString())).thenReturn(validAiResponse());
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, bankId, null);
      assertTrue(r.getAnalysisSuccessful());
      verify(questionTemplateRepository, times(1)).save(any(QuestionTemplate.class));
    }

    @Test
    void it_should_allow_non_owner_when_jwt_scopes_contain_admin() throws Exception {
      UUID me = UUID.fromString("70707070-7070-7070-7070-707070707070");
      Jwt jwt =
          new Jwt(
              "t",
              Instant.now(),
              Instant.now().plusSeconds(600),
              Map.of("alg", "RS256"),
              Map.of("sub", me.toString(), "scope", "ROLE_TEACHER ROLE_ADMIN"));
      SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(200L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("a.txt");
      when(multipartFile.getBytes())
          .thenReturn("1) A) 1".getBytes(StandardCharsets.UTF_8));
      UUID bankId = UUID.fromString("80808080-8080-8080-8080-808080808080");
      QuestionBank bank = new QuestionBank();
      bank.setId(bankId);
      bank.setTeacherId(UUID.fromString("90909090-9090-9090-9090-909090909090"));
      bank.setName("priv");
      bank.setIsPublic(false);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(geminiService.sendMessage(anyString())).thenReturn(validAiResponse());
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      assertTrue(
          templateImportService
              .importTemplateFromFile(multipartFile, null, null, bankId, null)
              .getAnalysisSuccessful());
    }

    @Test
    void it_should_return_error_when_security_context_has_no_jwt_for_user_id() throws Exception {
      SecurityContextHolder.clearContext();
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(50L);
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getOriginalFilename()).thenReturn("a.txt");
      when(multipartFile.getBytes()).thenReturn("body".getBytes(StandardCharsets.UTF_8));

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, null, null);
      assertFalse(r.getAnalysisSuccessful());
      assertTrue(r.getWarnings().get(0).toLowerCase().contains("process"));
    }

    @Test
    void it_should_return_error_when_legacy_msword_type_cannot_be_extracted() throws Exception {
      UUID userId = UUID.fromString("b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1");
      mockJwtAuth(userId);
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getSize()).thenReturn(80L);
      when(multipartFile.getContentType()).thenReturn("application/msword");
      when(multipartFile.getOriginalFilename()).thenReturn("legacy.doc");
      when(multipartFile.getBytes()).thenReturn(new byte[] {0x01, 0x02});

      TemplateImportResponse r =
          templateImportService.importTemplateFromFile(multipartFile, null, null, null, null);
      assertFalse(r.getAnalysisSuccessful());
    }
  }
}
