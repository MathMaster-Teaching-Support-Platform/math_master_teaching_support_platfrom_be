package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateCurriculumRequest;
import com.fptu.math_master.dto.request.UpdateCurriculumRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CurriculumResponse;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.service.CurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/curriculums")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Curriculum", description = "APIs for managing curricula")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

  CurriculumService curriculumService;

  @Operation(
      summary = "Create curriculum",
      description = "Admin creates a new curriculum for a specific grade and category")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<CurriculumResponse> createCurriculum(
      @Valid @RequestBody CreateCurriculumRequest request) {
    log.info("POST /curricula – creating curriculum: {}", request.getName());
    return ApiResponse.<CurriculumResponse>builder()
        .result(curriculumService.createCurriculum(request))
        .build();
  }

  @Operation(summary = "Update curriculum", description = "Admin updates curriculum information")
  @PutMapping("/{curriculumId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<CurriculumResponse> updateCurriculum(
      @PathVariable UUID curriculumId, @Valid @RequestBody UpdateCurriculumRequest request) {
    log.info("PUT /curricula/{} – updating curriculum", curriculumId);
    return ApiResponse.<CurriculumResponse>builder()
        .result(curriculumService.updateCurriculum(curriculumId, request))
        .build();
  }

  @Operation(summary = "Get curriculum by ID", description = "Get curriculum details by ID")
  @GetMapping("/{curriculumId}")
  public ApiResponse<CurriculumResponse> getCurriculumById(@PathVariable UUID curriculumId) {
    log.info("GET /curricula/{} – fetching curriculum", curriculumId);
    return ApiResponse.<CurriculumResponse>builder()
        .result(curriculumService.getCurriculumById(curriculumId))
        .build();
  }

  @Operation(
      summary = "Get all curricula (paginated)",
      description = "Get all curricula with pagination")
  @GetMapping
  public ApiResponse<Page<CurriculumResponse>> getAllCurricula(
      @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed)")
          int page,
      @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
    log.info("GET /curricula – fetching paginated curricula (page={}, size={})", page, size);
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<CurriculumResponse>>builder()
        .result(curriculumService.getAllCurricula(pageable))
        .build();
  }

  @Operation(summary = "Get all curricula", description = "Get all curricula without pagination")
  @GetMapping("/all")
  public ApiResponse<List<CurriculumResponse>> getAllCurriculaList() {
    log.info("GET /curricula/all – fetching all curricula");
    return ApiResponse.<List<CurriculumResponse>>builder()
        .result(curriculumService.getAllCurricula())
        .build();
  }

  @Operation(
      summary = "Get curricula by grade",
      description = "Get all curricula for a specific grade")
  @GetMapping("/grade/{grade}")
  public ApiResponse<List<CurriculumResponse>> getCurriculaByGrade(
      @PathVariable @Parameter(description = "Grade level (1-12)") Integer grade) {
    log.info("GET /curricula/grade/{} – fetching curricula", grade);
    return ApiResponse.<List<CurriculumResponse>>builder()
        .result(curriculumService.getCurriculaByGrade(grade))
        .build();
  }

  @Operation(
      summary = "Get curricula by category",
      description = "Get all curricula for a specific category")
  @GetMapping("/category/{category}")
  public ApiResponse<List<CurriculumResponse>> getCurriculaByCategory(
      @PathVariable @Parameter(description = "Category (GEOMETRY, NUMERICAL)")
          CurriculumCategory category) {
    log.info("GET /curricula/category/{} – fetching curricula", category);
    return ApiResponse.<List<CurriculumResponse>>builder()
        .result(curriculumService.getCurriculaByCategory(category))
        .build();
  }

  @Operation(
      summary = "Get curricula by grade and category",
      description = "Get all curricula for a specific grade and category combination")
  @GetMapping("/grade/{grade}/category/{category}")
  public ApiResponse<List<CurriculumResponse>> getCurriculaByGradeAndCategory(
      @PathVariable @Parameter(description = "Grade level (1-12)") Integer grade,
      @PathVariable @Parameter(description = "Category (GEOMETRY, NUMERICAL)")
          CurriculumCategory category) {
    log.info("GET /curricula/grade/{}/category/{} – fetching curricula", grade, category);
    return ApiResponse.<List<CurriculumResponse>>builder()
        .result(curriculumService.getCurriculaByGradeAndCategory(grade, category))
        .build();
  }

  @Operation(summary = "Search curricula", description = "Search curricula by name")
  @GetMapping("/search")
  public ApiResponse<List<CurriculumResponse>> searchCurricula(
      @RequestParam @Parameter(description = "Search term") String name) {
    log.info("GET /curricula/search – searching curricula by name: {}", name);
    return ApiResponse.<List<CurriculumResponse>>builder()
        .result(curriculumService.searchCurriculaByName(name))
        .build();
  }

  @Operation(summary = "Delete curriculum", description = "Admin deletes a curriculum")
  @DeleteMapping("/{curriculumId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> deleteCurriculum(@PathVariable UUID curriculumId) {
    log.info("DELETE /curricula/{} – deleting curriculum", curriculumId);
    curriculumService.deleteCurriculum(curriculumId);
    return ApiResponse.<Void>builder().message("Curriculum deleted successfully").build();
  }
}
