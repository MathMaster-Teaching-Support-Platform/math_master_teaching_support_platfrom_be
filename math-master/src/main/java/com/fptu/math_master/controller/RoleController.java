package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.RoleCreationRequest;
import com.fptu.math_master.dto.request.RoleUpdateRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.RoleResponse;
import com.fptu.math_master.service.RoleService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new role",
            description = "Create a new role with optional permissions. Only accessible by ADMIN role."
    )
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreationRequest request) {
        log.info("REST request to create role: {}", request.getName());
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.createRole(request))
                .build();
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update role by ID",
            description = "Update an existing role's information by role ID. Only accessible by ADMIN role."
    )
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable Integer roleId,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("REST request to update role: {}", roleId);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.updateRole(roleId, request))
                .build();
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete role by ID",
            description = "Delete a role by role ID. Only accessible by ADMIN role."
    )
    public ApiResponse<Void> deleteRole(@PathVariable Integer roleId) {
        log.info("REST request to delete role: {}", roleId);
        roleService.deleteRole(roleId);
        return ApiResponse.<Void>builder()
                .message("Role deleted successfully")
                .build();
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get role by ID",
            description = "Retrieve role information by role ID including its permissions."
    )
    public ApiResponse<RoleResponse> getRoleById(@PathVariable Integer roleId) {
        log.info("REST request to get role: {}", roleId);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getRoleById(roleId))
                .build();
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get role by name",
            description = "Retrieve role information by role name."
    )
    public ApiResponse<RoleResponse> getRoleByName(@PathVariable String name) {
        log.info("REST request to get role by name: {}", name);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getRoleByName(name))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all roles",
            description = "Retrieve a list of all roles. Only accessible by ADMIN role."
    )
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        log.info("REST request to get all roles");
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRoles())
                .build();
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all roles with pagination",
            description = "Retrieve a paginated list of all roles. Only accessible by ADMIN role."
    )
    public ApiResponse<Page<RoleResponse>> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        log.info("REST request to get all roles with pagination");

        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ApiResponse.<Page<RoleResponse>>builder()
                .result(roleService.getAllRoles(pageable))
                .build();
    }

    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add permissions to role",
            description = "Add one or more permissions to an existing role. Only accessible by ADMIN role."
    )
    public ApiResponse<RoleResponse> addPermissionsToRole(
            @PathVariable Integer roleId,
            @RequestBody List<String> permissionCodes) {
        log.info("REST request to add permissions to role: {}", roleId);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.addPermissionsToRole(roleId, permissionCodes))
                .build();
    }

    @DeleteMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove permissions from role",
            description = "Remove one or more permissions from an existing role. Only accessible by ADMIN role."
    )
    public ApiResponse<RoleResponse> removePermissionsFromRole(
            @PathVariable Integer roleId,
            @RequestBody List<String> permissionCodes) {
        log.info("REST request to remove permissions from role: {}", roleId);
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.removePermissionsFromRole(roleId, permissionCodes))
                .build();
    }
}

