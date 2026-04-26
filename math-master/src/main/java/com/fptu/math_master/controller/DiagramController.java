package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.dto.response.LatexRenderResponse;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.service.LatexRenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides a cache-friendly GET endpoint for rendering LaTeX diagrams.
 * Results are stored in {@code diagram_cache} so repeated calls for the same
 * formula never hit QuickLaTeX again.
 *
 * <p>Usage: {@code GET /diagrams/render?latex=\frac{x}{2}}
 */
@RestController
@RequestMapping("/diagrams")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class DiagramController {

  LatexRenderService latexRenderService;

  /**
   * Renders a LaTeX formula and returns the image URL. The result is cached in the
   * {@code diagram_cache} table so subsequent requests for the same formula are served
   * without calling QuickLaTeX.
   *
   * @param latex URL-encoded LaTeX string (no surrounding {@code $} delimiters needed)
   * @return {@link LatexRenderResponse} containing {@code imageUrl} on success
   */
  @GetMapping("/render")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Render LaTeX diagram (GET, cached)",
      description =
          "Cache-friendly GET endpoint for rendering LaTeX to an image URL. "
              + "First call hits QuickLaTeX; subsequent calls with the same formula are served "
              + "from the diagram_cache table. Suitable for <img> src or lazy-loading scenarios.")
  public ResponseEntity<LatexRenderResponse> renderDiagram(@RequestParam String latex) {
    if (latex == null || latex.isBlank()) {
      return ResponseEntity.badRequest()
          .body(LatexRenderResponse.builder().success(false).error("latex param must not be blank").build());
    }
    try {
      LatexRenderRequest request = LatexRenderRequest.builder().latex(latex).build();
      String imageUrl = latexRenderService.render(request);
      return ResponseEntity.ok(LatexRenderResponse.builder().success(true).imageUrl(imageUrl).build());
    } catch (LatexCompileException e) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (LatexRenderTimeoutException e) {
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(LatexRenderResponse.builder().success(false).error(e.getMessage()).build());
    } catch (Exception e) {
      log.error("Diagram render failed", e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(LatexRenderResponse.builder().success(false).error("Failed to render diagram.").build());
    }
  }
}
