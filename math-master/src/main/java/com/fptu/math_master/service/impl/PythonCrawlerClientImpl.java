package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.UpdateLessonPageRequest;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.PythonCrawlerClient;
import java.util.Collections;
import java.util.List;
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
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class PythonCrawlerClientImpl implements PythonCrawlerClient {

  private static final String BASE = "/api/v1";

  private final RestClient restClient;

  public PythonCrawlerClientImpl(@Qualifier("crawlDataRestClient") RestClient restClient) {
    this.restClient = restClient;
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
  public OcrStatus getBookOcrStatus(UUID bookId) {
    try {
      OcrStatus status =
          restClient
              .get()
              .uri(BASE + "/books/{bookId}/ocr-status", bookId)
              .retrieve()
              .body(OcrStatus.class);
      return status == null ? new OcrStatus("UNKNOWN", 0, 0, null) : status;
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    } catch (HttpClientErrorException.NotFound nf) {
      // Book has no OCR run yet — that's a valid state, not an error.
      return new OcrStatus("NOT_STARTED", 0, 0, null);
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
      return pages == null ? Collections.emptyList() : pages;
    } catch (HttpClientErrorException.NotFound nf) {
      return Collections.emptyList();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
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
      return pages == null ? Collections.emptyList() : pages;
    } catch (HttpClientErrorException.NotFound nf) {
      return Collections.emptyList();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
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
      return Optional.ofNullable(page);
    } catch (HttpClientErrorException.NotFound nf) {
      return Optional.empty();
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
  }

  @Override
  public LessonPageResponse updatePage(
      UUID bookId, UUID lessonId, int pageNumber, UpdateLessonPageRequest request, UUID actorId) {
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
              .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                if (resp.getStatusCode() == HttpStatus.NOT_FOUND) {
                  throw new AppException(ErrorCode.LESSON_PAGE_NOT_FOUND);
                }
                throw new AppException(ErrorCode.INVALID_REQUEST);
              })
              .body(LessonPageResponse.class);
      if (updated == null) {
        throw new AppException(ErrorCode.LESSON_PAGE_NOT_FOUND);
      }
      return updated;
    } catch (ResourceAccessException ex) {
      throw new AppException(ErrorCode.CRAWLER_UNAVAILABLE);
    }
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
}
