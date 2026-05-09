package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.UpdateLessonPageRequest;
import com.fptu.math_master.dto.response.ContentBlockDto;
import com.fptu.math_master.dto.response.LessonPageHistoryEntryResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.PythonCrawlerClient;
import com.fptu.math_master.service.UploadService;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class PythonCrawlerClientImpl implements PythonCrawlerClient {

  private static final String BASE = "/api/v1";
  private static final String STATIC_PREFIX = "/static/";
  private static final String STATIC_PROXY_PREFIX = "/api/v1/crawl-data/static/";

  private final RestClient restClient;
  private final UploadService uploadService;
  private final MinioProperties minioProperties;

  public PythonCrawlerClientImpl(
      @Qualifier("crawlDataRestClient") RestClient restClient,
      UploadService uploadService,
      MinioProperties minioProperties) {
    this.restClient = restClient;
    this.uploadService = uploadService;
    this.minioProperties = minioProperties;
  }

  @Override
  public OcrTriggerResult triggerOcrWithMapping(OcrTriggerRequest request) {
    try {
      OcrTriggerResult result =
          restClient
              .post()
              .uri(BASE + "/books/{bookId}/ocr-with-mapping", request.bookId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(OcrTriggerResult.class);
      return result == null ? new OcrTriggerResult("UNKNOWN", null, 0) : result;
    } catch (ResourceAccessException ex) {
      log.error("Crawler unreachable when triggering OCR for book {}", request.bookId(), ex);
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      log.error(
          "Crawler returned {} for OCR trigger of book {}: {}",
          ex.getStatusCode(),
          request.bookId(),
          ex.getResponseBodyAsString());
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public OcrTriggerResult triggerSinglePageOcr(OcrSinglePageTriggerRequest request) {
    try {
      OcrTriggerResult result =
          restClient
              .post()
              .uri(BASE + "/books/{bookId}/ocr-single-page", request.bookId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(OcrTriggerResult.class);
      return result == null ? new OcrTriggerResult("UNKNOWN", null, 0) : result;
    } catch (ResourceAccessException ex) {
      log.error(
          "Crawler unreachable when triggering single-page OCR for book {}", request.bookId(), ex);
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      log.error(
          "Crawler returned {} for single-page OCR of book {}: {}",
          ex.getStatusCode(),
          request.bookId(),
          ex.getResponseBodyAsString());
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public void cancelOcr(UUID bookId) {
    try {
      restClient
          .post()
          .uri(BASE + "/books/{bookId}/ocr-cancel", bookId)
          .retrieve()
          .toBodilessEntity();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      log.warn(
          "Crawler returned {} for OCR cancel of book {}: {}",
          ex.getStatusCode(),
          bookId,
          ex.getResponseBodyAsString());
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public OcrStatus getBookOcrStatus(UUID bookId) {
    try {
      OcrStatus status =
          restClient
              .get()
              .uri(BASE + "/books/{bookId}/ocr-status", bookId)
              .retrieve()
              .body(OcrStatus.class);
      return status == null ? new OcrStatus("UNKNOWN", 0, 0, null, null, null) : status;
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return new OcrStatus("NOT_STARTED", 0, 0, null, null, null);
      }
      log.warn(
          "Crawler returned {} for OCR status book={}: {}",
          ex.getStatusCode(),
          bookId,
          shortResponseBody(ex));
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientException ex) {
      log.warn("Rest client error reading OCR status book={}: {}", bookId, ex.getMessage());
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public List<LessonPageResponse> getPagesByBookAndLesson(UUID bookId, UUID lessonId) {
    try {
      List<LessonPageResponse> pages =
          restClient
              .get()
              .uri(BASE + "/books/{bookId}/lessons/{lessonId}/pages", bookId, lessonId)
              .retrieve()
              .body(new ParameterizedTypeReference<List<LessonPageResponse>>() {});
      return pages == null ? Collections.emptyList() : normalizePages(pages);
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Collections.emptyList();
      }
      log.warn(
          "Crawler returned {} listing pages for book={}, lesson={}: {}",
          ex.getStatusCode(),
          bookId,
          lessonId,
          shortResponseBody(ex));
      return Collections.emptyList();
    } catch (RestClientException ex) {
      log.warn(
          "Rest client error listing pages for book={}, lesson={}: {}",
          bookId,
          lessonId,
          ex.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public List<LessonPageResponse> getPagesByLesson(UUID lessonId, UUID bookId) {
    try {
      List<LessonPageResponse> pages =
          restClient
              .get()
              .uri(
                  uriBuilder -> {
                    var b = uriBuilder.path(BASE + "/lessons/{lessonId}/pages");
                    if (bookId != null) {
                      b.queryParam("book_id", bookId);
                    }
                    return b.build(lessonId);
                  })
              .retrieve()
              .body(new ParameterizedTypeReference<List<LessonPageResponse>>() {});
      return pages == null ? Collections.emptyList() : normalizePages(pages);
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return Collections.emptyList();
      }
      log.warn(
          "Crawler returned {} listing pages for lesson={}, bookId={}: {}",
          ex.getStatusCode(),
          lessonId,
          bookId,
          shortResponseBody(ex));
      return Collections.emptyList();
    } catch (RestClientException ex) {
      log.warn(
          "Rest client error listing pages for lesson={}, bookId={}: {}",
          lessonId,
          bookId,
          ex.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<LessonPageResponse> getPage(UUID bookId, UUID lessonId, int pageNumber) {
    try {
      LessonPageResponse page =
          restClient
              .get()
              .uri(
                  BASE + "/books/{bookId}/lessons/{lessonId}/pages/{pageNumber}",
                  bookId,
                  lessonId,
                  pageNumber)
              .retrieve()
              .body(LessonPageResponse.class);
      return Optional.ofNullable(page).map(this::normalizePage);
    } catch (HttpClientErrorException.NotFound nf) {
      return Optional.empty();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public List<LessonPageHistoryEntryResponse> getPageHistory(
      UUID bookId, UUID lessonId, int pageNumber, int limit) {
    try {
      List<LessonPageHistoryEntryResponse> entries =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(BASE + "/books/{bookId}/lessons/{lessonId}/pages/{pageNumber}/history")
                          .queryParam("limit", Math.max(1, limit))
                          .build(bookId, lessonId, pageNumber))
              .retrieve()
              .body(new ParameterizedTypeReference<List<LessonPageHistoryEntryResponse>>() {});
      return entries == null ? Collections.emptyList() : entries;
    } catch (HttpClientErrorException.NotFound nf) {
      return Collections.emptyList();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public LessonPageResponse updatePage(
      UUID bookId, UUID lessonId, int pageNumber, UpdateLessonPageRequest request, UUID actorId) {
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        LessonPageResponse updated =
            restClient
                .patch()
                .uri(
                    uriBuilder ->
                        uriBuilder
                            .path(BASE + "/books/{bookId}/lessons/{lessonId}/pages/{pageNumber}")
                            .queryParamIfPresent("actor_id", Optional.ofNullable(actorId))
                            .build(bookId, lessonId, pageNumber))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(
                    HttpStatusCode::is4xxClientError,
                    (req, resp) -> {
                      if (resp.getStatusCode() == HttpStatus.NOT_FOUND) {
                        throw new AppException(ErrorCode.LESSON_PAGE_NOT_FOUND);
                      }
                      throw new AppException(ErrorCode.INVALID_REQUEST);
                    })
                .body(LessonPageResponse.class);
        if (updated == null) {
          throw new AppException(ErrorCode.LESSON_PAGE_NOT_FOUND);
        }
        return normalizePage(updated);
      } catch (ResourceAccessException ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
          root = root.getCause();
        }
        String rootMsg =
            root.getClass().getSimpleName() + (root.getMessage() == null ? "" : (": " + root.getMessage()));
        if (attempt >= 2) {
          log.warn(
              "Crawler unreachable when updating page. bookId={}, lessonId={}, pageNumber={}, cause={}",
              bookId,
              lessonId,
              pageNumber,
              rootMsg);
          throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
        }
        log.warn(
            "Crawler unreachable on attempt {}/2 when updating page. Retrying... bookId={}, lessonId={}, pageNumber={}, cause={}",
            attempt,
            bookId,
            lessonId,
            pageNumber,
            rootMsg);
        try {
          Thread.sleep(250L);
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
          throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
        }
      }
    }
    throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
  }

  @Override
  public void deleteAllPagesForBook(UUID bookId) {
    try {
      restClient
          .delete()
          .uri(BASE + "/books/{bookId}/pages", bookId)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException.NotFound nf) {
      log.debug("No pages to delete for book {}", bookId);
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public boolean isBookFullyVerified(UUID bookId) {
    try {
      VerifyState state =
          restClient
              .get()
              .uri(BASE + "/books/{bookId}/verification", bookId)
              .retrieve()
              .body(VerifyState.class);
      return state != null && Boolean.TRUE.equals(state.fullyVerified());
    } catch (HttpClientErrorException.NotFound nf) {
      return false;
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  /** Internal — must match Python /verification response shape. */
  private record VerifyState(Boolean fullyVerified, Integer totalPages, Integer verifiedPages) {}

  private List<LessonPageResponse> normalizePages(List<LessonPageResponse> pages) {
    Map<String, String> mediaUrlCache = new HashMap<>();
    pages.forEach(page -> normalizePage(page, mediaUrlCache));
    return pages;
  }

  private LessonPageResponse normalizePage(LessonPageResponse page) {
    return normalizePage(page, new HashMap<>());
  }

  private LessonPageResponse normalizePage(LessonPageResponse page, Map<String, String> mediaUrlCache) {
    if (page == null) {
      return null;
    }
    page.setRawImageUrl(resolveMediaUrl(page.getRawImageUrl(), mediaUrlCache));
    if (page.getContentBlocks() != null) {
      page.getContentBlocks().forEach(block -> normalizeBlockUrls(block, mediaUrlCache));
    }
    return page;
  }

  private void normalizeBlockUrls(ContentBlockDto block, Map<String, String> mediaUrlCache) {
    String imageUrl = resolveMediaUrl(block.getImageUrl(), mediaUrlCache);
    String imagePath = resolveMediaUrl(block.getImagePath(), mediaUrlCache);
    String thumbnailUrl = resolveMediaUrl(block.getThumbnailUrl(), mediaUrlCache);

    block.setImageUrl(firstNonBlank(imageUrl, imagePath));
    block.setImagePath(imagePath);
    block.setThumbnailUrl(thumbnailUrl);
  }

  private static String shortResponseBody(RestClientResponseException ex) {
    String body = ex.getResponseBodyAsString();
    if (body == null || body.isEmpty()) {
      return "";
    }
    int max = 500;
    return body.length() <= max ? body : body.substring(0, max) + "...";
  }

  private String resolveMediaUrl(String value, Map<String, String> mediaUrlCache) {
    String normalized = rewriteStaticPath(value);
    if (normalized == null || normalized.isBlank()) {
      return normalized;
    }
    if (normalized.startsWith(STATIC_PROXY_PREFIX)
        || normalized.startsWith("http://")
        || normalized.startsWith("https://")) {
      return normalized;
    }

    String cached = mediaUrlCache.get(normalized);
    if (cached != null) {
      return cached;
    }

    // Remaining non-static strings are treated as private object keys.
    try {
      String presigned = uploadService.getPresignedUrl(normalized, minioProperties.getOcrContentBucket());
      mediaUrlCache.put(normalized, presigned);
      return presigned;
    } catch (Exception ex) {
      log.warn("Failed to presign OCR media key '{}': {}", normalized, ex.getMessage());
      mediaUrlCache.put(normalized, normalized);
      return normalized;
    }
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) return first;
    return second;
  }

  private String rewriteStaticPath(String path) {
    if (path == null || path.isBlank()) {
      return path;
    }
    if (path.startsWith(STATIC_PROXY_PREFIX) || path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    if (path.startsWith(STATIC_PREFIX)) {
      return STATIC_PROXY_PREFIX + path.substring(STATIC_PREFIX.length());
    }
    return path;
  }
}
