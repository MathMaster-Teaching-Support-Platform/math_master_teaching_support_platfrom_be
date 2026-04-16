package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.GenerateMindmapRequest;
import com.fptu.math_master.dto.request.MindmapNodeRequest;
import com.fptu.math_master.dto.request.MindmapRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.MindmapDetailResponse;
import com.fptu.math_master.dto.response.MindmapNodeResponse;
import com.fptu.math_master.dto.response.MindmapResponse;
import com.fptu.math_master.service.MindmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mindmaps")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mindmap", description = "Mindmap management endpoints with AI generation support")
public class MindmapController {

  MindmapService mindmapService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create a new mindmap",
      description =
          "Teacher creates a new empty mindmap. Can optionally link to a lesson. "
              + "Status is DRAFT by default. Nodes can be added manually after creation.")
  public ApiResponse<MindmapResponse> createMindmap(@Valid @RequestBody MindmapRequest request) {
    log.info("REST request to create mindmap: {}", request.getTitle());
    return ApiResponse.<MindmapResponse>builder()
        .message("Mindmap created successfully. You can now add nodes to your mindmap.")
        .result(mindmapService.createMindmap(request))
        .build();
  }

  @PostMapping("/generate")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate mindmap using AI",
      description =
          "Teacher provides a topic/prompt and AI (Gemini) generates a complete mindmap structure. "
              + "The AI creates a hierarchical node structure with colors, icons, and organized branches. "
              + "You can specify the number of levels (depth) for the mindmap (2-6 levels, default is 3). "
              + "Generated mindmap is saved as DRAFT and can be edited afterwards.")
  public ApiResponse<MindmapDetailResponse> generateMindmap(
      @Valid @RequestBody GenerateMindmapRequest request) {
    log.info("REST request to generate mindmap from AI");
    return ApiResponse.<MindmapDetailResponse>builder()
        .message(
            "Mindmap generated successfully using AI. Review and edit the structure as needed.")
        .result(mindmapService.generateMindmap(request))
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get mindmap by ID",
      description =
          "Retrieve a mindmap with all its nodes in hierarchical structure. "
              + "Returns mindmap details and complete node tree.")
  public ApiResponse<MindmapDetailResponse> getMindmapById(@PathVariable UUID id) {
    log.info("REST request to get mindmap: {}", id);
    return ApiResponse.<MindmapDetailResponse>builder()
        .result(mindmapService.getMindmapById(id))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update mindmap",
      description =
          "Update mindmap details (title, description, lesson link). "
              + "Only the owner can update their mindmap.")
  public ApiResponse<MindmapResponse> updateMindmap(
      @PathVariable UUID id, @Valid @RequestBody MindmapRequest request) {
    log.info("REST request to update mindmap: {}", id);
    return ApiResponse.<MindmapResponse>builder()
        .message("Mindmap updated successfully")
        .result(mindmapService.updateMindmap(id, request))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete mindmap",
      description =
          "Soft delete a mindmap. The mindmap and all its nodes will be marked as deleted.")
  public ApiResponse<Void> deleteMindmap(@PathVariable UUID id) {
    log.info("REST request to delete mindmap: {}", id);
    mindmapService.deleteMindmap(id);
    return ApiResponse.<Void>builder().message("Mindmap deleted successfully").build();
  }

  @PatchMapping("/{id}/publish")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Publish mindmap",
      description =
          "Change mindmap status to PUBLISHED. Published mindmaps can be viewed by students.")
  public ApiResponse<MindmapResponse> publishMindmap(@PathVariable UUID id) {
    log.info("REST request to publish mindmap: {}", id);
    return ApiResponse.<MindmapResponse>builder()
        .message("Mindmap published successfully")
        .result(mindmapService.publishMindmap(id))
        .build();
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Archive mindmap",
      description =
          "Change mindmap status to ARCHIVED. Archived mindmaps are hidden from active lists.")
  public ApiResponse<MindmapResponse> archiveMindmap(@PathVariable UUID id) {
    log.info("REST request to archive mindmap: {}", id);
    return ApiResponse.<MindmapResponse>builder()
        .message("Mindmap archived successfully")
        .result(mindmapService.archiveMindmap(id))
        .build();
  }

  @GetMapping("/my-mindmaps")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get my mindmaps",
      description =
          "Get all mindmaps created by the current teacher. Optionally filter by lessonId. Supports pagination and sorting.")
  public ApiResponse<Page<MindmapResponse>> getMyMindmaps(
      @RequestParam(required = false) String lessonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String direction) {

    // Convert lessonId string to UUID if provided
    UUID lessonUuid = null;
    if (lessonId != null && !lessonId.trim().isEmpty()) {
      try {
        lessonUuid = UUID.fromString(lessonId);
      } catch (IllegalArgumentException e) {
        log.warn("Invalid lessonId format: {}", lessonId);
        // lessonUuid remains null, which means no filtering by lesson
      }
    }

    log.info(
        "REST request to get my mindmaps - lessonId: {}, page: {}, size: {}",
        lessonUuid,
        page,
        size);

    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<MindmapResponse>>builder()
        .result(mindmapService.getMyMindmaps(lessonUuid, pageable))
        .build();
  }

  @GetMapping("/lesson/{lessonId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Get mindmaps by lesson",
      description = "Get all mindmaps linked to a specific lesson. Supports pagination.")
  public ApiResponse<Page<MindmapResponse>> getMindmapsByLesson(
      @PathVariable UUID lessonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("REST request to get mindmaps for lesson: {}", lessonId);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    return ApiResponse.<Page<MindmapResponse>>builder()
        .result(mindmapService.getMindmapsByLesson(lessonId, pageable))
        .build();
  }

  @GetMapping("/public")
  @Operation(
      summary = "List all public mindmaps",
      description = "Public endpoint for students to browse all published mindmaps.")
  public ApiResponse<Page<MindmapResponse>> getPublicMindmaps(
      @RequestParam(required = false) UUID lessonId,
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String direction) {
    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<MindmapResponse>>builder()
        .result(mindmapService.getPublicMindmaps(lessonId, name, pageable))
        .build();
  }

  @GetMapping("/{id}/export")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Export mindmap as image or PDF",
      description = "Teacher exports own mindmap in PNG or PDF format for download.")
  public ResponseEntity<byte[]> exportMindmap(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "pdf") String format) {
    MindmapService.BinaryFileData fileData = mindmapService.exportMindmap(id, format);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @GetMapping("/public/{id}/export")
  @Operation(
      summary = "Export published mindmap as image or PDF",
      description = "Public endpoint for students to download published mindmaps in PNG or PDF.")
  public ResponseEntity<byte[]> exportPublicMindmap(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "pdf") String format) {
    MindmapService.BinaryFileData fileData = mindmapService.exportPublicMindmap(id, format);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  // Node management endpoints

  @PostMapping("/nodes")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create a node",
      description =
          "Add a new node to a mindmap. Can be a root node or child of an existing node. "
              + "Specify content, color, icon, and display order.")
  public ApiResponse<MindmapNodeResponse> createNode(
      @Valid @RequestBody MindmapNodeRequest request) {
    log.info("REST request to create mindmap node");
    return ApiResponse.<MindmapNodeResponse>builder()
        .message("Node created successfully")
        .result(mindmapService.createNode(request))
        .build();
  }

  @PutMapping("/nodes/{nodeId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update a node",
      description = "Update node content, color, icon, or display order.")
  public ApiResponse<MindmapNodeResponse> updateNode(
      @PathVariable UUID nodeId, @Valid @RequestBody MindmapNodeRequest request) {
    log.info("REST request to update node: {}", nodeId);
    return ApiResponse.<MindmapNodeResponse>builder()
        .message("Node updated successfully")
        .result(mindmapService.updateNode(nodeId, request))
        .build();
  }

  @DeleteMapping("/nodes/{nodeId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete a node",
      description = "Delete a node and all its children. This operation cannot be undone.")
  public ApiResponse<Void> deleteNode(@PathVariable UUID nodeId) {
    log.info("REST request to delete node: {}", nodeId);
    mindmapService.deleteNode(nodeId);
    return ApiResponse.<Void>builder().message("Node deleted successfully").build();
  }

  @GetMapping("/{mindmapId}/nodes")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Get all nodes of a mindmap",
      description =
          "Get all nodes in hierarchical structure. Root nodes contain their children recursively.")
  public ApiResponse<List<MindmapNodeResponse>> getNodesByMindmap(@PathVariable UUID mindmapId) {
    log.info("REST request to get nodes for mindmap: {}", mindmapId);
    return ApiResponse.<List<MindmapNodeResponse>>builder()
        .result(mindmapService.getNodesByMindmap(mindmapId))
        .build();
  }

    private String contentDisposition(String fileName) {
        return "attachment; filename*=UTF-8''"
                + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
