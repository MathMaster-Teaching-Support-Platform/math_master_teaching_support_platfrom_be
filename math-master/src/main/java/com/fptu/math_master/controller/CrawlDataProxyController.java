package com.fptu.math_master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

@RestController
@RequestMapping("/v1/crawl-data")
@Slf4j
@Tag(name = "Crawl Data", description = "Proxy to crawl-data service — SGK books, chapters, lessons, search, chat, ranking, university")
public class CrawlDataProxyController {

    @Value("${crawl-data.base-url:http://localhost:8001}")
    private String crawlDataBaseUrl;

    @Value("${crawl-data.internal-api-key:change-me-in-production}")
    private String internalApiKey;

    private final RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());

    private static final String PROXY_PREFIX = "/api/v1/crawl-data";

    // =========================================================
    // BOOKS
    // =========================================================

    @Operation(summary = "[Books] Upload PDF book and start background processing",
               description = "POST /api/v1/books/upload — multipart: file (PDF), grade, publisher, title, academic_year")
    @PostMapping("/books/upload")
    public ResponseEntity<byte[]> booksUpload(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] List all books", description = "GET /api/v1/books/?grade=&status=")
    @GetMapping("/books")
    public ResponseEntity<byte[]> booksList(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Get book detail by ID")
    @GetMapping("/books/{bookId}")
    public ResponseEntity<byte[]> booksGet(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Get processing status of a book (for polling)")
    @GetMapping("/books/{bookId}/status")
    public ResponseEntity<byte[]> booksStatus(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] List all chapters of a book")
    @GetMapping("/books/{bookId}/chapters")
    public ResponseEntity<byte[]> booksChapters(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Export book as JSON tree")
    @GetMapping("/books/{bookId}/export/json")
    public ResponseEntity<byte[]> booksExportJson(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Export book as Markdown")
    @GetMapping("/books/{bookId}/export/md")
    public ResponseEntity<byte[]> booksExportMd(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Export book as RAG-ready chunks")
    @GetMapping("/books/{bookId}/export/chunks")
    public ResponseEntity<byte[]> booksExportChunks(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Delete a book and all associated data")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<byte[]> booksDelete(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Update book metadata (title, grade, publisher, academic_year)")
    @PatchMapping("/books/{bookId}")
    public ResponseEntity<byte[]> booksUpdate(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Reprocess book from cached OCR (STEP 3+4 only)")
    @PostMapping("/books/{bookId}/reprocess")
    public ResponseEntity<byte[]> booksReprocess(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] OCR preview — upload 1-5 page images, get Gemini analysis")
    @PostMapping("/books/ocr-preview")
    public ResponseEntity<byte[]> booksOcrPreview(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // CHAPTERS
    // =========================================================

    @Operation(summary = "[Chapters] Get chapter by ID")
    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<byte[]> chaptersGet(
            @Parameter(description = "Chapter ID") @PathVariable String chapterId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chapters] List all lessons in a chapter")
    @GetMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<byte[]> chaptersLessons(
            @Parameter(description = "Chapter ID") @PathVariable String chapterId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chapters] Update chapter title / roman index")
    @PatchMapping("/chapters/{chapterId}")
    public ResponseEntity<byte[]> chaptersUpdate(
            @Parameter(description = "Chapter ID") @PathVariable String chapterId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chapters] Create a new chapter in a book")
    @PostMapping("/chapters")
    public ResponseEntity<byte[]> chaptersCreate(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chapters] Get change history for a chapter")
    @GetMapping("/chapters/{chapterId}/history")
    public ResponseEntity<byte[]> chaptersHistory(
            @Parameter(description = "Chapter ID") @PathVariable String chapterId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chapters] Delete chapter and cascade lessons/content")
    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<byte[]> chaptersDelete(
            @Parameter(description = "Chapter ID") @PathVariable String chapterId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // LESSONS
    // =========================================================

    @Operation(summary = "[Lessons] Get lesson by ID")
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<byte[]> lessonsGet(
            @Parameter(description = "Lesson ID") @PathVariable String lessonId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Lessons] Get all content blocks of a lesson")
    @GetMapping("/lessons/{lessonId}/content")
    public ResponseEntity<byte[]> lessonsContent(
            @Parameter(description = "Lesson ID") @PathVariable String lessonId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Lessons] Update lesson title")
    @PatchMapping("/lessons/{lessonId}")
    public ResponseEntity<byte[]> lessonsUpdate(
            @Parameter(description = "Lesson ID") @PathVariable String lessonId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Lessons] Create a new lesson in a chapter")
    @PostMapping("/lessons")
    public ResponseEntity<byte[]> lessonsCreate(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Lessons] Get change history for a lesson")
    @GetMapping("/lessons/{lessonId}/history")
    public ResponseEntity<byte[]> lessonsHistory(
            @Parameter(description = "Lesson ID") @PathVariable String lessonId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Lessons] Delete lesson and its content blocks")
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<byte[]> lessonsDelete(
            @Parameter(description = "Lesson ID") @PathVariable String lessonId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // CONTENT BLOCKS
    // =========================================================

    @Operation(summary = "[Content] Get a single content block by ID")
    @GetMapping("/content/{contentId}")
    public ResponseEntity<byte[]> contentGet(
            @Parameter(description = "Content block ID") @PathVariable String contentId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Content] Create a new content block in a lesson")
    @PostMapping("/content")
    public ResponseEntity<byte[]> contentCreate(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Content] Update a content block")
    @PatchMapping("/content/{contentId}")
    public ResponseEntity<byte[]> contentUpdate(
            @Parameter(description = "Content block ID") @PathVariable String contentId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Content] Delete a content block")
    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<byte[]> contentDelete(
            @Parameter(description = "Content block ID") @PathVariable String contentId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Content] Get change history for a content block")
    @GetMapping("/content/{contentId}/history")
    public ResponseEntity<byte[]> contentHistory(
            @Parameter(description = "Content block ID") @PathVariable String contentId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Books] Get full change history for a book")
    @GetMapping("/books/{bookId}/history")
    public ResponseEntity<byte[]> booksHistory(
            @Parameter(description = "Book ID") @PathVariable String bookId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Operation(summary = "[Search] Full-text search across lesson content",
               description = "Params: q (required), grade (optional), chapter_id (optional), limit (default 20)")
    @GetMapping("/search")
    public ResponseEntity<byte[]> search(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // CHAT
    // =========================================================

    @Operation(summary = "[Chat] Create a new chat session")
    @PostMapping("/chat/session")
    public ResponseEntity<byte[]> chatCreateSession(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chat] Send a message and get streaming response",
               description = "Body: { session_id, user_message }. Returns text/plain stream.")
    @PostMapping("/chat/message")
    public ResponseEntity<byte[]> chatMessage(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[Chat] Get chat history for a session")
    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<byte[]> chatHistory(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // RANKING
    // =========================================================

    @Operation(summary = "[Ranking] Tra cứu ranking THPTQG 2025 theo số báo danh",
               description = "Body: { candidate_number (8 digits), region (CN/MB/MT/MN) }")
    @PostMapping("/ranking/thptqg/2025/search")
    public ResponseEntity<byte[]> rankingSearch(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // UNIVERSITY
    // =========================================================

    @Operation(summary = "[University] Get all universities from database")
    @GetMapping("/university/all")
    public ResponseEntity<byte[]> universityAll(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[University] Search university by code or name")
    @GetMapping("/university/search")
    public ResponseEntity<byte[]> universitySearch(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[University] Fetch & update universities from external API")
    @PostMapping("/university/update")
    public ResponseEntity<byte[]> universityUpdate(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[University] Create a new university")
    @PostMapping("/university/create")
    public ResponseEntity<byte[]> universityCreate(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    @Operation(summary = "[University] Update a university by ID")
    @PutMapping("/university/update/{uniId}")
    public ResponseEntity<byte[]> universityUpdateById(
            @Parameter(description = "University numeric ID") @PathVariable String uniId,
            HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // DEMO OCR
    // =========================================================

    @Operation(summary = "[Demo] OCR demo — upload 1 page image, get Gemini analysis instantly")
    @PostMapping("/demo/ocr")
    public ResponseEntity<byte[]> demoOcr(HttpServletRequest request) throws IOException {
        return forward(request);
    }

    // =========================================================
    // STATIC FILES (images served by Python service)
    // =========================================================

    @Operation(summary = "[Static] Proxy static image files from crawl-data service",
               description = "Proxies /static/images/... served by Python FastAPI directly (no /api/v1 prefix).")
    @GetMapping("/static/**")
    public ResponseEntity<byte[]> staticFiles(HttpServletRequest request) throws IOException {
        return forwardStatic(request);
    }

    // =========================================================
    // PROXY CORE
    // =========================================================

    private ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {
        String requestUri = request.getRequestURI();
        String downstreamPath = requestUri.startsWith(PROXY_PREFIX)
                ? requestUri.substring(PROXY_PREFIX.length())
                : requestUri;

        String queryString = request.getQueryString();
        String targetUrl = crawlDataBaseUrl + "/api/v1" + downstreamPath
                + (queryString != null ? "?" + queryString : "");

        log.debug("CrawlDataProxy: {} {} -> {}", request.getMethod(), requestUri, targetUrl);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!isHopByHopHeader(name)) {
                    headers.put(name, Collections.list(request.getHeaders(name)));
                }
            }
        }
        // Inject internal API key so Python service rejects direct external calls
        headers.set("X-Internal-API-Key", internalApiKey);

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpEntity<byte[]> entity = (body.length > 0) ? new HttpEntity<>(body, headers)
                                                       : new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(
                    URI.create(targetUrl),
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsByteArray());
        }
    }

    /**
     * Forwards /api/v1/crawl-data/static/** to {crawlDataBaseUrl}/static/**
     * without prepending /api/v1, because Python FastAPI serves images at the root /static path.
     */
    private ResponseEntity<byte[]> forwardStatic(HttpServletRequest request) throws IOException {
        String requestUri = request.getRequestURI();
        // Strip /api/v1/crawl-data prefix to get /static/...
        String downstreamPath = requestUri.startsWith(PROXY_PREFIX)
                ? requestUri.substring(PROXY_PREFIX.length())
                : requestUri;

        String queryString = request.getQueryString();
        String targetUrl = crawlDataBaseUrl + downstreamPath
                + (queryString != null ? "?" + queryString : "");

        log.debug("CrawlDataProxy[static]: {} -> {}", requestUri, targetUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-API-Key", internalApiKey);

        try {
            return restTemplate.exchange(
                    URI.create(targetUrl),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsByteArray());
        }
    }

    private boolean isHopByHopHeader(String name) {
        return switch (name.toLowerCase()) {
            case "connection", "keep-alive", "proxy-authenticate",
                 "proxy-authorization", "te", "trailer",
                 "transfer-encoding", "upgrade" -> true;
            default -> false;
        };
    }
}
