package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.GeminiProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@DisplayName("GeminiServiceImpl - Tests")
class GeminiServiceImplTest extends BaseUnitTest {

  @Mock private RestClient geminiRestClient;
  @Mock private GeminiProperties geminiProperties;
  @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RestClient.RequestBodySpec requestBodySpec;
  @Mock private RestClient.ResponseSpec responseSpec;
  @Mock private RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientHttpResponse;

  private GeminiServiceImpl geminiService;

  private static final String SUCCESS_RAW_RESPONSE =
      """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                { "text": "Xin chao" }
              ]
            }
          }
        ]
      }
      """;

  @BeforeEach
  void setUp() {
    geminiService = new GeminiServiceImpl(geminiRestClient, geminiProperties);
  }

  @SuppressWarnings("rawtypes")
  private void mockExchangeResponse(int statusCode, String responseBody) throws Exception {
    when(geminiRestClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);

    when(clientHttpResponse.getBody())
        .thenReturn(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
    HttpStatusCode httpStatusCode = HttpStatusCode.valueOf(statusCode);
    when(clientHttpResponse.getStatusCode()).thenReturn(httpStatusCode);

    when(requestBodySpec.exchange(any()))
        .thenAnswer(
            invocation -> {
              RestClient.RequestHeadersSpec.ExchangeFunction exchangeFunction =
                  invocation.getArgument(0);
              return exchangeFunction.exchange(null, clientHttpResponse);
            });
  }

  private void mockExchangeReturningNullBody() {
    when(geminiRestClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.exchange(any())).thenReturn(null);
  }

  @Nested
  @DisplayName("sendMessage()")
  class SendMessageTests {

    /**
     * Normal case: Gọi Gemini API thành công và lấy được text content.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>prompt: "Giải thích định lý Pythagoras"
     *   <li>response HTTP: 200, body chứa candidates.parts[0].text
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>statusCode == 200 (FALSE branch của check HTTP != 200)
     *   <li>raw != null/blank (FALSE branch của empty raw check)
     *   <li>content != null/blank (FALSE branch của empty content check)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về nội dung text "Xin chao"
     * </ul>
     */
    @Test
    void it_should_return_text_content_when_gemini_api_returns_valid_response() throws Exception {
      // ===== ARRANGE =====
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, SUCCESS_RAW_RESPONSE);

      // ===== ACT =====
      String result = geminiService.sendMessage("Giải thích định lý Pythagoras");

      // ===== ASSERT =====
      assertEquals("Xin chao", result);

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodyUriSpec, times(1)).uri(any(String.class));
      verify(requestBodySpec, times(1)).contentType(MediaType.APPLICATION_JSON);
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Gemini API trả về HTTP status khác 200.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 500 với body lỗi
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>statusCode != 200 (TRUE branch, throw RuntimeException trong exchange callback)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to communicate with Gemini API"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_gemini_api_returns_non_200_status() throws Exception {
      // ===== ARRANGE =====
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(500, "{\"error\":\"Internal error\"}");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(RuntimeException.class, () -> geminiService.sendMessage("Tạo đề cương bài học"));
      assertTrue(exception.getMessage().contains("Failed to communicate with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Gemini API trả về body rỗng.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 200, body chỉ chứa khoảng trắng
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>raw == null || raw.isBlank() (TRUE branch, throw empty response)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message lỗi bọc ngoài từ service
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_gemini_api_returns_empty_raw_body() throws Exception {
      // ===== ARRANGE =====
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, "   ");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> geminiService.sendMessage("Hãy viết lời chào ngắn"));
      assertTrue(exception.getMessage().contains("Failed to communicate with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Exchange callback trả về null body.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>exchange(...) trả về null
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>raw == null || raw.isBlank() (TRUE branch qua raw == null)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to communicate with Gemini API"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_exchange_returns_null_raw_body() {
      // ===== ARRANGE =====
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeReturningNullBody();

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> geminiService.sendMessage("Viết một câu chào"));
      assertTrue(exception.getMessage().contains("Failed to communicate with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Gemini response không có text content hợp lệ.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 200, JSON không có parts.text
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>content == null || content.isBlank() (TRUE branch, throw no text content)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to communicate with Gemini API"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_gemini_response_has_no_text_content() throws Exception {
      // ===== ARRANGE =====
      String rawWithoutText = "{\"candidates\":[{\"content\":{\"parts\":[{}]}}]}";
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, rawWithoutText);

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> geminiService.sendMessage("Tóm tắt chương trình hình học"));
      assertTrue(exception.getMessage().contains("Failed to communicate with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }
  }

  @Nested
  @DisplayName("testConnection()")
  class TestConnectionTests {

    /**
     * Normal case: testConnection trả về true khi sendMessage có nội dung.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>sendMessage("Say hello in one word.") trả về "Hello"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>result != null &amp;&amp; !result.isBlank() (TRUE branch)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về true
     * </ul>
     */
    @Test
    void it_should_return_true_when_test_connection_receives_non_blank_message() {
      // ===== ARRANGE =====
      GeminiServiceImpl serviceSpy = spy(new GeminiServiceImpl(geminiRestClient, geminiProperties));
      doReturn("Hello").when(serviceSpy).sendMessage("Say hello in one word.");

      // ===== ACT =====
      boolean result = serviceSpy.testConnection();

      // ===== ASSERT =====
      assertTrue(result);

      // ===== VERIFY =====
      verify(serviceSpy, times(1)).sendMessage("Say hello in one word.");
      verifyNoMoreInteractions(geminiRestClient, geminiProperties);
    }

    /**
     * Abnormal case: sendMessage ném exception trong testConnection.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>sendMessage("Say hello in one word.") ném RuntimeException
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>catch branch của testConnection() -> return false
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về false, không rethrow exception
     * </ul>
     */
    @Test
    void it_should_return_false_when_send_message_throws_exception_in_test_connection() {
      // ===== ARRANGE =====
      GeminiServiceImpl serviceSpy = spy(new GeminiServiceImpl(geminiRestClient, geminiProperties));
      doThrow(new RuntimeException("Connection timeout"))
          .when(serviceSpy)
          .sendMessage("Say hello in one word.");

      // ===== ACT =====
      boolean result = serviceSpy.testConnection();

      // ===== ASSERT =====
      assertFalse(result);

      // ===== VERIFY =====
      verify(serviceSpy, times(1)).sendMessage("Say hello in one word.");
      verifyNoMoreInteractions(geminiRestClient, geminiProperties);
    }

    /**
     * Abnormal case: sendMessage trả về chuỗi rỗng trong testConnection.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>sendMessage("Say hello in one word.") trả về "  "
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>result != null &amp;&amp; !result.isBlank() (FALSE branch do isBlank == true)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về false
     * </ul>
     */
    @Test
    void it_should_return_false_when_test_connection_receives_blank_message() {
      // ===== ARRANGE =====
      GeminiServiceImpl serviceSpy = spy(new GeminiServiceImpl(geminiRestClient, geminiProperties));
      doReturn("  ").when(serviceSpy).sendMessage("Say hello in one word.");

      // ===== ACT =====
      boolean result = serviceSpy.testConnection();

      // ===== ASSERT =====
      assertFalse(result);

      // ===== VERIFY =====
      verify(serviceSpy, times(1)).sendMessage("Say hello in one word.");
      verifyNoMoreInteractions(geminiRestClient, geminiProperties);
    }
  }

  @Nested
  @DisplayName("analyzeImageWithPrompt()")
  class AnalyzeImageWithPromptTests {

    /**
     * Normal case: Phân tích ảnh thành công và trả về text.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>imageBytes: dữ liệu ảnh JPEG giả lập
     *   <li>prompt: "Extract text from this certificate"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>statusCode == 200 (FALSE branch của HTTP error)
     *   <li>raw không rỗng, content không rỗng (happy path)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về nội dung text từ JSON response
     * </ul>
     */
    @Test
    void it_should_return_text_when_image_analysis_response_is_valid() throws Exception {
      // ===== ARRANGE =====
      byte[] imageBytes = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, SUCCESS_RAW_RESPONSE);

      // ===== ACT =====
      String result = geminiService.analyzeImageWithPrompt(imageBytes, "Extract text from image");

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals("Xin chao", result);

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Gemini trả về HTTP non-200 trong image analysis.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 429 Too Many Requests
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>statusCode != 200 (TRUE branch, throw từ exchange callback)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to analyze image"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_image_analysis_returns_non_200_status()
        throws Exception {
      // ===== ARRANGE =====
      byte[] imageBytes = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(429, "{\"error\":\"Quota exceeded\"}");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> geminiService.analyzeImageWithPrompt(imageBytes, "Extract text from image"));
      assertTrue(exception.getMessage().contains("Failed to analyze image with Gemini API"));

      // ===== VERIFY =====
      // 429 is retried up to MAX_RETRY_ATTEMPTS; each attempt builds URI with getModel/getApiKey.
      verify(geminiProperties, times(5)).getModel();
      verify(geminiProperties, times(4)).getApiKey();
      verify(geminiRestClient, times(4)).post();
      verify(requestBodySpec, times(4)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Image analysis trả về raw body rỗng.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 200, body = " "
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>raw == null || raw.isBlank() (TRUE branch qua isBlank)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to analyze image"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_image_analysis_returns_blank_raw_body()
        throws Exception {
      // ===== ARRANGE =====
      byte[] imageBytes = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, " ");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> geminiService.analyzeImageWithPrompt(imageBytes, "Extract text from image"));
      assertTrue(exception.getMessage().contains("Failed to analyze image with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }

    /**
     * Abnormal case: Image analysis JSON không có text content.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>response HTTP: 200, JSON không có parts.text
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>content == null || content.isBlank() (TRUE branch)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Failed to analyze image"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_image_analysis_response_has_no_text_content()
        throws Exception {
      // ===== ARRANGE =====
      byte[] imageBytes = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
      when(geminiProperties.getModel()).thenReturn("gemini-2.0-flash");
      when(geminiProperties.getApiKey()).thenReturn("demo-api-key");
      mockExchangeResponse(200, "{\"candidates\":[{\"content\":{\"parts\":[{}]}}]}");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> geminiService.analyzeImageWithPrompt(imageBytes, "Extract text from image"));
      assertTrue(exception.getMessage().contains("Failed to analyze image with Gemini API"));

      // ===== VERIFY =====
      verify(geminiProperties, times(2)).getModel();
      verify(geminiProperties, times(1)).getApiKey();
      verify(geminiRestClient, times(1)).post();
      verify(requestBodySpec, times(1)).exchange(any());
      verifyNoMoreInteractions(geminiProperties, geminiRestClient);
    }
  }
}
