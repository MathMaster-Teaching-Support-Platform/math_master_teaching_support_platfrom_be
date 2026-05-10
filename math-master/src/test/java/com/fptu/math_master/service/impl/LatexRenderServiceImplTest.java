package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.entity.DiagramCache;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderProxyException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.repository.DiagramCacheRepository;
import com.fptu.math_master.repository.QuestionRepository;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Method;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("LatexRenderServiceImpl - Tests")
class LatexRenderServiceImplTest extends BaseUnitTest {

  @InjectMocks private LatexRenderServiceImpl service;

  @Mock private QuestionRepository questionRepository;
  @Mock private DiagramCacheRepository diagramCacheRepository;
  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<String> httpResponse;

  private UUID questionId;
  private Question question;

  @BeforeEach
  void setUp() {
    questionId = UUID.randomUUID();
    question = new Question();
    question.setId(questionId);
    question.setCreatedAt(Instant.now());
    try {
      java.lang.reflect.Field field = LatexRenderServiceImpl.class.getDeclaredField("httpClient");
      field.setAccessible(true);
      field.set(service, httpClient);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Abnormal case: Rejects blank latex input.
   *
   * <p>Input:
   * <ul>
   *   <li>latex: blank string</li>
   * </ul>
   *
   * <p>Branch coverage:
   * <ul>
   *   <li>{@code rawLatex == null || rawLatex.isBlank()} -> TRUE branch</li>
   * </ul>
   *
   * <p>Expectation:
   * <ul>
   *   <li>Throws {@link IllegalArgumentException}</li>
   * </ul>
   */
  @Test
  void it_should_throw_exception_when_latex_is_blank() {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("   ").build();

    // ===== ACT & ASSERT =====
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.render(request));

    assertEquals("latex must not be blank", exception.getMessage());

    // ===== VERIFY =====
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Normal case: Returns question-level cache when hash matches.
   */
  @Test
  void it_should_return_question_cached_url_when_hash_matches() {
    // ===== ARRANGE =====
    question.setRenderedLatexHash(sha256("x+y"));
    question.setRenderedImageUrl("https://cdn.math/render1.png");
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y").questionId(questionId).build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://cdn.math/render1.png", result);

    // ===== VERIFY =====
    verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId);
    verify(diagramCacheRepository, never()).findByLatexHash(any());
    verify(questionRepository, never())
        .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            any());
    verify(questionRepository, never()).save(any());
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Normal case: Uses question cache when rendered hash is null but URL exists.
   */
  @Test
  void it_should_return_question_cached_url_when_question_hash_is_null() {
    // ===== ARRANGE =====
    question.setRenderedLatexHash(null);
    question.setRenderedImageUrl("https://cdn.math/render-null-hash.png");
    LatexRenderRequest request = LatexRenderRequest.builder().latex("z^2").questionId(questionId).build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://cdn.math/render-null-hash.png", result);

    // ===== VERIFY =====
    verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId);
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Abnormal case: Throws when questionId is provided but target question does not exist.
   */
  @Test
  void it_should_throw_exception_when_question_id_is_not_found() {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y").questionId(questionId).build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.render(request));
    assertEquals("Question not found: " + questionId, exception.getMessage());
  }

  /**
   * Normal case: Reads URL from standalone diagram cache.
   */
  @Test
  void it_should_return_diagram_cache_url_when_found() {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("\\frac{a}{b}").build();
    DiagramCache cache = DiagramCache.builder().imageUrl("https://cdn.math/diagram-cache.png").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.of(cache));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://cdn.math/diagram-cache.png", result);

    // ===== VERIFY =====
    verify(diagramCacheRepository, times(1)).findByLatexHash(any());
    verify(questionRepository, never()).save(any());
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Normal case: Falls through question cache when rendered image URL is blank.
   */
  @Test
  void it_should_bypass_question_cache_when_question_rendered_url_is_blank() {
    // ===== ARRANGE =====
    question.setRenderedImageUrl("   ");
    question.setRenderedLatexHash("any-hash");
    LatexRenderRequest request = LatexRenderRequest.builder().latex("\\frac{a}{b}").questionId(questionId).build();
    DiagramCache cache = DiagramCache.builder().imageUrl("https://cdn.math/diagram-cache-2.png").build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.of(cache));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://cdn.math/diagram-cache-2.png", result);
  }

  /**
   * Normal case: Uses global question cache and stores diagram cache.
   */
  @Test
  void it_should_return_global_cache_and_persist_diagram_cache_when_present() {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("a^2+b^2").build();
    Question cachedQuestion = new Question();
    cachedQuestion.setRenderedImageUrl("https://cdn.math/global-cache.png");
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.of(cachedQuestion));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://cdn.math/global-cache.png", result);

    // ===== VERIFY =====
    verify(questionRepository, times(1))
        .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            any());
    verify(diagramCacheRepository, times(2)).findByLatexHash(any());
    verify(diagramCacheRepository, times(1)).save(any(DiagramCache.class));
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Normal case: Calls QuickLaTeX when no cache is hit and persists both caches.
   */
  @Test
  void it_should_call_quick_latex_and_persist_caches_when_no_cache_exists() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x^2 + y^2").questionId(questionId).build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/rendered.png 0 0 0");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/rendered.png", result);
    assertNotNull(question.getRenderedLatexHash());
    assertEquals("https://quicklatex.com/cache/rendered.png", question.getRenderedImageUrl());

    // ===== VERIFY =====
    verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId);
    verify(questionRepository, times(1))
        .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            any());
    verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    verify(questionRepository, times(1)).save(eq(question));
    verify(diagramCacheRepository, times(2)).findByLatexHash(any());
    verify(diagramCacheRepository, times(1)).save(any(DiagramCache.class));
    verifyNoMoreInteractions(questionRepository, diagramCacheRepository, httpClient);
  }

  /**
   * Abnormal case: Throws compile exception when QuickLaTeX returns status 1.
   */
  @Test
  void it_should_throw_compile_exception_when_quick_latex_returns_compile_error() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("\\sqrt{").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("1\nMissing closing brace");

    // ===== ACT & ASSERT =====
    LatexCompileException exception =
        assertThrows(LatexCompileException.class, () -> service.render(request));

    assertEquals("Missing closing brace", exception.getMessage());

    // ===== VERIFY =====
    verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    verify(questionRepository, never()).save(any());
    verify(diagramCacheRepository, never()).save(any());
  }

  /**
   * Abnormal case: Throws timeout exception when QuickLaTeX request times out.
   */
  @Test
  void it_should_throw_timeout_exception_when_quick_latex_times_out() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new HttpTimeoutException("timed out"));

    // ===== ACT & ASSERT =====
    assertThrows(LatexRenderTimeoutException.class, () -> service.render(request));

    // ===== VERIFY =====
    verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  /**
   * Abnormal case: Throws proxy exception when QuickLaTeX returns non-2xx status.
   */
  @Test
  void it_should_throw_proxy_exception_when_quick_latex_returns_non_2xx() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(503);
    when(httpResponse.body()).thenReturn("Service unavailable");

    // ===== ACT & ASSERT =====
    LatexRenderProxyException exception =
        assertThrows(LatexRenderProxyException.class, () -> service.render(request));

    assertEquals("QuickLaTeX returned HTTP 503", exception.getMessage());
  }

  /**
   * Abnormal case: Throws proxy exception when response format has fewer than 2 lines.
   */
  @Test
  void it_should_throw_proxy_exception_when_response_format_is_invalid() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0");

    // ===== ACT & ASSERT =====
    LatexRenderProxyException exception =
        assertThrows(LatexRenderProxyException.class, () -> service.render(request));

    assertEquals("Invalid response format from QuickLaTeX.", exception.getMessage());
  }

  /**
   * Abnormal case: Throws proxy exception when status is unknown.
   */
  @Test
  void it_should_throw_proxy_exception_when_status_is_unknown() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("2\nsomething");

    // ===== ACT & ASSERT =====
    LatexRenderProxyException exception =
        assertThrows(LatexRenderProxyException.class, () -> service.render(request));

    assertEquals("Unexpected QuickLaTeX status: 2", exception.getMessage());
  }

  /**
   * Abnormal case: QuickLaTeX status -1 (generic failure image) maps to compile-style exception.
   */
  @Test
  void it_should_throw_compile_exception_when_quicklatex_returns_status_minus_one() throws Exception {
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("-1\nhttps://quicklatex.com/cache3/error.png 0 0 0");

    LatexCompileException exception =
        assertThrows(LatexCompileException.class, () -> service.render(request));

    assertNotNull(exception.getMessage());
    assertEquals(true, exception.getMessage().contains("status -1"));
  }

  /**
   * Abnormal case: Throws proxy exception when response payload does not contain an image URL.
   */
  @Test
  void it_should_throw_proxy_exception_when_payload_has_blank_image_url() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\n   ");

    // ===== ACT & ASSERT =====
    LatexRenderProxyException exception =
        assertThrows(LatexRenderProxyException.class, () -> service.render(request));
    assertEquals("QuickLaTeX response did not include image URL.", exception.getMessage());
  }

  /**
   * Abnormal case: Throws proxy exception when http client fails with non-timeout exception.
   */
  @Test
  void it_should_throw_proxy_exception_when_http_client_throws_unexpected_exception() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y+z").build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new RuntimeException("connection refused"));

    // ===== ACT & ASSERT =====
    LatexRenderProxyException exception =
        assertThrows(LatexRenderProxyException.class, () -> service.render(request));
    assertEquals("Failed to call QuickLaTeX API.", exception.getMessage());
  }

  /**
   * Normal case: Does not write diagram cache when hash already exists at persist step.
   */
  @Test
  void it_should_skip_diagram_cache_insert_when_hash_exists_during_persist() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x^2+y^2").questionId(questionId).build();
    DiagramCache existing = DiagramCache.builder().imageUrl("https://cached/existing.png").build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.of(existing));
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/exists.png");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/exists.png", result);

    // ===== VERIFY =====
    verify(diagramCacheRepository, times(2)).findByLatexHash(any());
    verify(diagramCacheRepository, never()).save(any(DiagramCache.class));
  }

  /**
   * Normal case: Returns rendered URL even when diagram cache persistence throws exception.
   */
  @Test
  void it_should_continue_render_flow_when_diagram_cache_persist_throws_exception() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x^2+y^2").questionId(questionId).build();
    when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/persist-error.png");
    when(diagramCacheRepository.save(any(DiagramCache.class))).thenThrow(new RuntimeException("db down"));

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/persist-error.png", result);
    verify(questionRepository, times(1)).save(eq(question));
  }

  /**
   * Normal case: Uses explicit render options (font, color, mode, preamble) when provided.
   */
  @Test
  void it_should_use_explicit_options_when_render_options_are_provided() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest.LatexRenderOptions options =
        LatexRenderRequest.LatexRenderOptions.builder()
            .fontSize("20px")
            .color("FF0000")
            .mode(1)
            .preamble("\\usepackage{amsmath}")
            .build();
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y").options(options).build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/opts.png");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/opts.png", result);
  }

  /**
   * Normal case: Uses default preamble when formula contains tikz and custom preamble is blank.
   */
  @Test
  void it_should_use_default_tikz_preamble_when_formula_contains_tikz_and_preamble_blank()
      throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest.LatexRenderOptions options =
        LatexRenderRequest.LatexRenderOptions.builder().fontSize(" ").color(" ").preamble(" ").build();
    LatexRenderRequest request =
        LatexRenderRequest.builder().latex("\\begin{tikzpicture}\\draw(0,0)--(1,1);").options(options).build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/tikz-default.png");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/tikz-default.png", result);
  }

  /**
   * Normal case: Uses empty preamble when formula has no tikz and options preamble is blank.
   */
  @Test
  void it_should_use_empty_preamble_when_formula_has_no_tikz_and_preamble_blank() throws Exception {
    // ===== ARRANGE =====
    LatexRenderRequest.LatexRenderOptions options =
        LatexRenderRequest.LatexRenderOptions.builder().fontSize("").color("").mode(null).preamble("").build();
    LatexRenderRequest request = LatexRenderRequest.builder().latex("x+y").options(options).build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty(), Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/empty-preamble.png");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/empty-preamble.png", result);
  }

  /**
   * Normal case: Balanced-brace sqrt + frac / nested sqrt collapse so QuickLaTeX sees numeric coords.
   */
  @Test
  void it_should_normalize_sqrt_frac_and_nested_sqrt_inside_tikz() throws Exception {
    Method normalize =
        LatexRenderServiceImpl.class.getDeclaredMethod("normalizeLatexForRender", String.class);
    normalize.setAccessible(true);

    String frac =
        "\\begin{tikzpicture}\\draw (0,0)--(\\sqrt{\\frac{1}{4}},0);\\end{tikzpicture}";
    String outFrac = (String) normalize.invoke(service, frac);
    assertEquals(true, outFrac.contains("0.5") || outFrac.contains(".5"), outFrac);

    String nested =
        "\\begin{tikzpicture}\\draw (0,0)--(\\sqrt{\\sqrt{16}},0);\\end{tikzpicture}";
    String outNest = (String) normalize.invoke(service, nested);
    assertEquals(true, outNest.contains("(2,0)") || outNest.contains("(2, 0)"), outNest);
  }

  /**
   * Normal case: Handles malformed tikz command by normalizing before hashing/rendering.
   */
  @Test
  void it_should_normalize_tikz_typo_before_processing() throws Exception {
    // ===== ARRANGE =====
    String malformed = "\\begintikzpicture\\draw (0,0)--(1,1);\\endtikzpicture";
    LatexRenderRequest request = LatexRenderRequest.builder().latex(malformed).build();
    when(diagramCacheRepository.findByLatexHash(any())).thenReturn(Optional.empty());
    when(
            questionRepository
                .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
                    any()))
        .thenReturn(Optional.empty());
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("0\nhttps://quicklatex.com/cache/tikz.png");

    // ===== ACT =====
    String result = service.render(request);

    // ===== ASSERT =====
    assertEquals("https://quicklatex.com/cache/tikz.png", result);
    ArgumentCaptor<DiagramCache> captor = ArgumentCaptor.forClass(DiagramCache.class);
    verify(diagramCacheRepository).save(captor.capture());
    assertEquals(true, captor.getValue().getLatexContent().contains("\\begin{tikzpicture}"));
  }

  private String sha256(String input) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hashBytes.length * 2);
      for (byte b : hashBytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
