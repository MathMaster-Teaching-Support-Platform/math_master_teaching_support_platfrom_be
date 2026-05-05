package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.TokenCostConfigRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TokenCostConfigResponse;
import com.fptu.math_master.entity.TokenCostAuditLog;
import com.fptu.math_master.service.TokenCostConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/token-config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin — Token Config", description = "Admin APIs for managing feature token costs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTokenConfigController {

  TokenCostConfigService tokenCostConfigService;

  @Operation(summary = "List all feature token configs")
  @GetMapping
  public ApiResponse<List<TokenCostConfigResponse>> listAll() {
    return ApiResponse.<List<TokenCostConfigResponse>>builder()
        .result(tokenCostConfigService.getAllConfigs())
        .build();
  }

  @Operation(summary = "Update feature token cost or status")
  @PatchMapping("/{id}")
  public ApiResponse<TokenCostConfigResponse> update(
      @PathVariable UUID id, @Valid @RequestBody TokenCostConfigRequest request) {
    return ApiResponse.<TokenCostConfigResponse>builder()
        .result(tokenCostConfigService.updateConfig(id, request))
        .build();
  }

  @Operation(summary = "Get all token cost audit logs")
  @GetMapping("/history")
  public ApiResponse<List<TokenCostAuditLog>> getHistory() {
    return ApiResponse.<List<TokenCostAuditLog>>builder()
        .result(tokenCostConfigService.getAuditLogs())
        .build();
  }
}
