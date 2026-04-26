package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.dto.response.LatexRenderResponse;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.service.LatexRenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/latex")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class LatexRenderController {

  private static final HttpClient PROXY_HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  LatexRenderService latexRenderService;

  @PostMapping("/render")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Render LaTeX via QuickLaTeX proxy",
      description =
          "Proxies LaTeX rendering to QuickLaTeX, applies tikz preamble when needed, and caches image URL by latex hash.")
  public ResponseEntity<LatexRenderResponse> render(@Valid @RequestBody LatexRenderRequest request) {
    try {
      String imageUrl = latexRenderService.render(request);
      return ResponseEntity.ok(LatexRenderResponse.builder().success(true).imageUrl(imageUrl).build());
    } catch (LatexCompileException e) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (LatexRenderTimeoutException e) {
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (Exception e) {
      log.error("LaTeX render proxy failed", e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(
              LatexRenderResponse.builder()
                  .success(false)
                  .error("Failed to render LaTeX via proxy.")
                  .build());
    }
  }

  /**
   * Proxies a QuickLaTeX image URL back to the browser.
   * Solves CORS: quicklatex.com does not send Access-Control-Allow-Origin headers,
   * so browsers cannot fetch images directly. FE should call:
   *   /api/latex/image-proxy?url={encodedQuickLaTeXUrl}
   * instead of fetching quicklatex.com directly.
   */
  @GetMapping("/image-proxy")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(summary = "Proxy a QuickLaTeX image to avoid browser CORS restrictions")
  public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
    // Only allow proxying quicklatex.com URLs
    if (url == null || !url.startsWith("https://quicklatex.com/")) {
      return ResponseEntity.badRequest().build();
    }
    try {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(15))
          .GET()
          .build();
      HttpResponse<byte[]> resp = PROXY_HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
        log.warn("Image proxy received non-2xx from quicklatex: status={} url={}", resp.statusCode(), url);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
      }
      return ResponseEntity.ok()
          .contentType(MediaType.IMAGE_PNG)
          .body(resp.body());
    } catch (Exception e) {
      log.warn("Image proxy failed for url={}: {}", url, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
  }
}
