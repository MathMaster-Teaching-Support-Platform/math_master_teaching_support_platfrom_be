package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderProxyException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.LatexRenderService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LatexRenderServiceImpl implements LatexRenderService {

  private static final String QUICK_LATEX_URL = "https://quicklatex.com/latex3.f";
  private static final String DEFAULT_FONT_SIZE = "17px";
  private static final String DEFAULT_COLOR = "000000";
  private static final int DEFAULT_MODE = 0;
  private static final String DEFAULT_PREAMBLE =
      "\\usepackage{tikz}\n"
          + "\\usepackage{pgfplots}\n"
        + "\\usepackage{tkz-tab}\n"
          + "\\usepackage{tkz-euclide}\n"
          + "\\pgfplotsset{compat=newest}";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  QuestionRepository questionRepository;

  HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  @Override
  @Transactional
  public String render(LatexRenderRequest request) {
    String rawLatex = request.getLatex();
    if (rawLatex == null || rawLatex.isBlank()) {
      throw new IllegalArgumentException("latex must not be blank");
    }
    String normalizedLatex = normalizeLatexForRender(rawLatex);
    String latexHash = sha256(normalizedLatex);

    Question targetQuestion = resolveQuestion(request.getQuestionId());
    String questionCachedUrl = getQuestionLevelCachedUrl(targetQuestion, latexHash);
    if (questionCachedUrl != null) {
      return questionCachedUrl;
    }

    Optional<String> globalCachedUrl =
      questionRepository
        .findFirstByRenderedLatexHashAndRenderedImageUrlIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
          latexHash)
        .map(Question::getRenderedImageUrl);
    if (globalCachedUrl.isPresent()) {
      persistQuestionCache(targetQuestion, latexHash, globalCachedUrl.get());
      return globalCachedUrl.get();
    }

    String renderedImageUrl = callQuickLatex(normalizedLatex, request.getOptions(), rawLatex);
    persistQuestionCache(targetQuestion, latexHash, renderedImageUrl);
    return renderedImageUrl;
  }

  private String normalizeLatexForRender(String latex) {
    if (latex == null || latex.isBlank()) {
      return latex;
    }

    String fixed = latex;

    // Common AI typo: missing braces in tikz environment commands.
    fixed = fixed.replace("\\begintikzpicture", "\\begin{tikzpicture}");
    fixed = fixed.replace("\\endtikzpicture", "\\end{tikzpicture}");

    // Common AI typo in tkz-tab variation command: missing opening brace.
    fixed =
        fixed.replaceAll(
            "(?m)(\\\\tkzTabVar\\s*)(?!\\{)([^\\n]+\\})",
            "$1{$2");

    // Common AI typo in tkz-tab init: second argument line starts without opening '{'.
    fixed =
        fixed.replaceAll(
            "(?m)(\\\\tkzTabInit[^\\n]*\\n)(\\s*)(?!\\{)([-+]?\\\\infty[^\\n]*\\})",
            "$1$2{$3");

    if (!fixed.equals(latex)) {
      log.warn("Normalized malformed LaTeX before render");
    }

    return fixed;
  }

  private Question resolveQuestion(UUID questionId) {
    if (questionId == null) {
      return null;
    }
    return questionRepository
        .findByIdAndNotDeleted(questionId)
        .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
  }

  private String getQuestionLevelCachedUrl(Question question, String latexHash) {
    if (question == null || question.getRenderedImageUrl() == null || question.getRenderedImageUrl().isBlank()) {
      return null;
    }
    if (question.getRenderedLatexHash() == null || latexHash.equals(question.getRenderedLatexHash())) {
      return question.getRenderedImageUrl();
    }
    return null;
  }

  private void persistQuestionCache(Question question, String latexHash, String imageUrl) {
    if (question == null) {
      return;
    }
    question.setRenderedLatexHash(latexHash);
    question.setRenderedImageUrl(imageUrl);
    questionRepository.save(question);
  }

  private String callQuickLatex(
      String formula,
      LatexRenderRequest.LatexRenderOptions options,
      String rawLatexForLogging) {
    String fontSize =
        options != null && options.getFontSize() != null && !options.getFontSize().isBlank()
            ? options.getFontSize()
            : DEFAULT_FONT_SIZE;
    String color =
        options != null && options.getColor() != null && !options.getColor().isBlank()
            ? options.getColor()
            : DEFAULT_COLOR;
    int mode = options != null && options.getMode() != null ? options.getMode() : DEFAULT_MODE;
    String preamble = resolvePreamble(formula, options);

    Map<String, String> form = new LinkedHashMap<>();
    form.put("formula", formula);
    form.put("preamble", preamble);
    form.put("fsize", fontSize);
    form.put("fcolor", color);
    form.put("mode", String.valueOf(mode));
    form.put("out", "1");
    form.put("remhost", "quicklatex.com");

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(QUICK_LATEX_URL))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(toFormUrlEncoded(form)))

            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (HttpTimeoutException e) {
      log.error("QuickLaTeX timeout. rawLatex={} error={}", rawLatexForLogging, e.getMessage());
      throw new LatexRenderTimeoutException(
          "QuickLaTeX timeout while rendering formula. Please retry.", e);
    } catch (Exception e) {
      log.error("QuickLaTeX request failed. rawLatex={} error={}", rawLatexForLogging, e.getMessage(), e);
      throw new LatexRenderProxyException("Failed to call QuickLaTeX API.", e);
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      log.error(
          "QuickLaTeX non-2xx response. rawLatex={} status={} body={}",
          rawLatexForLogging,
          response.statusCode(),
          response.body());
      throw new LatexRenderProxyException("QuickLaTeX returned HTTP " + response.statusCode());
    }

    String body = response.body() == null ? "" : response.body();
    String[] lines = body.split("\\R");
    if (lines.length < 2) {
      log.error("QuickLaTeX invalid response. rawLatex={} body={}", rawLatexForLogging, body);
      throw new LatexRenderProxyException("Invalid response format from QuickLaTeX.");
    }

    String status = lines[0].trim();
    String payload = lines[1].trim();

    if ("1".equals(status)) {
      log.error("QuickLaTeX compile error. rawLatex={} error={}", rawLatexForLogging, payload);
      throw new LatexCompileException(payload);
    }

    if (!"0".equals(status)) {
      log.error(
          "QuickLaTeX unknown status. rawLatex={} status={} payload={}",
          rawLatexForLogging,
          status,
          payload);
      throw new LatexRenderProxyException("Unexpected QuickLaTeX status: " + status);
    }

    String[] parts = payload.split("\\s+");
    if (parts.length == 0 || parts[0].isBlank()) {
      log.error("QuickLaTeX missing image URL. rawLatex={} payload={}", rawLatexForLogging, payload);
      throw new LatexRenderProxyException("QuickLaTeX response did not include image URL.");
    }
    return parts[0];
  }

  private String toFormUrlEncoded(Map<String, String> form) {
    return form.entrySet().stream()
        .map(
            entry ->
                encodeFormComponent(entry.getKey())
                    + "="
                    + encodeFormComponent(entry.getValue() == null ? "" : entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private String encodeFormComponent(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String resolvePreamble(String formula, LatexRenderRequest.LatexRenderOptions options) {
    if (options != null && options.getPreamble() != null && !options.getPreamble().isBlank()) {
      return options.getPreamble();
    }
    if (formula != null && formula.contains("\\begin{tikzpicture}")) {
      return DEFAULT_PREAMBLE;
    }
    return "";
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hashBytes.length * 2);
      for (byte b : hashBytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new LatexRenderProxyException("SHA-256 algorithm is not available.", e);
    }
  }
}
