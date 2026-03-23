package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TeachingResourceResponse;
import com.fptu.math_master.enums.TeachingResourceType;
import com.fptu.math_master.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Teaching Resources", description = "Upload and manage teaching resources")
public class ResourceController {

  ResourceService resourceService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Upload resource file")
  public ApiResponse<TeachingResourceResponse> uploadResource(
      @RequestParam("name") String name,
      @RequestParam("type") TeachingResourceType type,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.<TeachingResourceResponse>builder()
        .message("Resource uploaded successfully")
        .result(resourceService.uploadResource(name, type, file))
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(summary = "Get resource by ID")
  public ApiResponse<TeachingResourceResponse> getResource(@PathVariable UUID id) {
    return ApiResponse.<TeachingResourceResponse>builder().result(resourceService.getResource(id)).build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Delete resource")
  public ApiResponse<Void> deleteResource(@PathVariable UUID id) {
    resourceService.deleteResource(id);
    return ApiResponse.<Void>builder().message("Resource deleted successfully").build();
  }
}
