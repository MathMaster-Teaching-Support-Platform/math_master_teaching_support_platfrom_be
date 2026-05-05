package com.fptu.math_master.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.request.UpdateSystemConfigRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SystemConfigResponse;
import com.fptu.math_master.service.SystemConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Admin endpoints for managing platform-wide system configuration.
 * All operations require the ADMIN role.
 */
@RestController
@RequestMapping("/admin/system-config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin — System Config", description = "Admin APIs for managing platform-wide configuration (privacy policy, etc.)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemConfigController {

    SystemConfigService systemConfigService;

    @Operation(summary = "List all system config entries")
    @GetMapping
    public ApiResponse<List<SystemConfigResponse>> listAll() {
        return ApiResponse.<List<SystemConfigResponse>>builder()
                .result(systemConfigService.listAll())
                .build();
    }

    @Operation(summary = "Get a single config entry by key")
    @GetMapping("/{key}")
    public ApiResponse<SystemConfigResponse> getByKey(@PathVariable String key) {
        return ApiResponse.<SystemConfigResponse>builder()
                .result(systemConfigService.getByKey(key))
                .build();
    }

    @Operation(summary = "Update the value of an existing config entry")
    @PutMapping("/{key}")
    public ApiResponse<SystemConfigResponse> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ApiResponse.<SystemConfigResponse>builder()
                .result(systemConfigService.update(key, request, adminId))
                .build();
    }
}
