package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.PermissionCreationRequest;
import com.fptu.math_master.dto.request.PermissionUpdateRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.PermissionResponse;
import com.fptu.math_master.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

  PermissionService permissionService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Create a new permission",
    description = "Create a new permission. Only accessible by ADMIN role."
  )
  public ApiResponse<PermissionResponse> createPermission(@Valid @RequestBody PermissionCreationRequest request) {
    log.info("REST request to create permission: {}", request.getCode());
    return ApiResponse.<PermissionResponse>builder()
      .result(permissionService.createPermission(request))
      .build();
  }

  @PutMapping("/{permissionId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Update permission by ID",
    description = "Update an existing permission's information by permission ID. Only accessible by ADMIN role."
  )
  public ApiResponse<PermissionResponse> updatePermission(
    @PathVariable Integer permissionId,
    @Valid @RequestBody PermissionUpdateRequest request) {
    log.info("REST request to update permission: {}", permissionId);
    return ApiResponse.<PermissionResponse>builder()
      .result(permissionService.updatePermission(permissionId, request))
      .build();
  }

  @DeleteMapping("/{permissionId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Delete permission by ID",
    description = "Delete a permission by permission ID. Only accessible by ADMIN role."
  )
  public ApiResponse<Void> deletePermission(@PathVariable Integer permissionId) {
    log.info("REST request to delete permission: {}", permissionId);
    permissionService.deletePermission(permissionId);
    return ApiResponse.<Void>builder()
      .message("Permission deleted successfully")
      .build();
  }

  @GetMapping("/{permissionId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Get permission by ID",
    description = "Retrieve permission information by permission ID."
  )
  public ApiResponse<PermissionResponse> getPermissionById(@PathVariable Integer permissionId) {
    log.info("REST request to get permission: {}", permissionId);
    return ApiResponse.<PermissionResponse>builder()
      .result(permissionService.getPermissionById(permissionId))
      .build();
  }

  @GetMapping("/code/{code}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Get permission by code",
    description = "Retrieve permission information by permission code."
  )
  public ApiResponse<PermissionResponse> getPermissionByCode(@PathVariable String code) {
    log.info("REST request to get permission by code: {}", code);
    return ApiResponse.<PermissionResponse>builder()
      .result(permissionService.getPermissionByCode(code))
      .build();
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Get all permissions",
    description = "Retrieve a list of all permissions. Only accessible by ADMIN role."
  )
  public ApiResponse<List<PermissionResponse>> getAllPermissions() {
    log.info("REST request to get all permissions");
    return ApiResponse.<List<PermissionResponse>>builder()
      .result(permissionService.getAllPermissions())
      .build();
  }

  @GetMapping("/page")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Get all permissions with pagination",
    description = "Retrieve a paginated list of all permissions. Only accessible by ADMIN role."
  )
  public ApiResponse<Page<PermissionResponse>> getAllPermissions(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") String sortDirection) {
    log.info("REST request to get all permissions with pagination");

    Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC")
      ? Sort.Direction.DESC
      : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ApiResponse.<Page<PermissionResponse>>builder()
      .result(permissionService.getAllPermissions(pageable))
      .build();
  }
}

