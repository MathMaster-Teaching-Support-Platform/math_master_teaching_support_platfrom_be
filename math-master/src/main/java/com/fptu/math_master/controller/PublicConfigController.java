package com.fptu.math_master.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SystemConfigResponse;
import com.fptu.math_master.service.SystemConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Public (unauthenticated) read-only endpoints for system config.
 * Currently used to serve the privacy policy content to the registration flow.
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public — Config", description = "Public read-only access to platform configuration (no auth required)")
public class PublicConfigController {

    SystemConfigService systemConfigService;

    @Operation(summary = "Get a config value by key")
    @GetMapping("/{key}")
    public ApiResponse<SystemConfigResponse> getConfig(@PathVariable String key) {
        return ApiResponse.<SystemConfigResponse>builder()
                .result(systemConfigService.getByKey(key))
                .build();
    }
}
