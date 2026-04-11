package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.dto.response.LatexRenderResponse;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.service.LatexRenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/latex")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class LatexRenderController {

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
}
