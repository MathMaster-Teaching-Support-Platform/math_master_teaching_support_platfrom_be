package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.TokenCostConfigRequest;
import com.fptu.math_master.dto.response.TokenCostConfigResponse;
import com.fptu.math_master.entity.TokenCostAuditLog;
import com.fptu.math_master.entity.TokenCostConfig;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TokenCostAuditLogRepository;
import com.fptu.math_master.repository.TokenCostConfigRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.TokenCostConfigService;
import com.fptu.math_master.util.SecurityUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenCostConfigServiceImpl implements TokenCostConfigService {

  TokenCostConfigRepository tokenCostConfigRepository;
  TokenCostAuditLogRepository tokenCostAuditLogRepository;
  UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public List<TokenCostConfigResponse> getAllConfigs() {
    return tokenCostConfigRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public TokenCostConfigResponse updateConfig(UUID id, TokenCostConfigRequest request) {
    TokenCostConfig config =
        tokenCostConfigRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND));

    UUID adminId = SecurityUtils.getCurrentUserId();
    String featureKey = config.getFeatureKey();

    if (request.getCostPerUse() != null && !request.getCostPerUse().equals(config.getCostPerUse())) {
      logAudit(
          adminId,
          featureKey,
          config.getCostPerUse().toString(),
          request.getCostPerUse().toString(),
          "COST_UPDATE");
      config.setCostPerUse(request.getCostPerUse());
    }

    if (request.getIsActive() != null && !request.getIsActive().equals(config.getIsActive())) {
      logAudit(
          adminId,
          featureKey,
          config.getIsActive().toString(),
          request.getIsActive().toString(),
          "STATUS_TOGGLE");
      config.setIsActive(request.getIsActive());
    }

    config.setUpdatedBy(adminId);
    return mapToResponse(tokenCostConfigRepository.save(config));
  }

  @Override
  public Integer getCostPerUse(String featureKey) {
    return tokenCostConfigRepository
        .findByFeatureKey(featureKey)
        .map(
            config -> {
              if (Boolean.TRUE.equals(config.getIsActive())) {
                return config.getCostPerUse();
              }
              return 0; // Or throw exception if feature is disabled? User said "deducted per feature usage". If disabled, maybe cost is 0 or feature itself should be blocked.
            })
        .orElse(0); // Default to 0 if not found
  }

  @Override
  public List<TokenCostAuditLog> getAuditLogs() {
    return tokenCostAuditLogRepository.findAllByOrderByCreatedAtDesc();
  }

  private void logAudit(
      UUID adminId, String featureKey, String oldValue, String newValue, String changeType) {
    String adminName = userRepository.findById(adminId)
        .map(user -> user.getFullName())
        .orElse("Hệ thống");

    TokenCostAuditLog auditLog =
        TokenCostAuditLog.builder()
            .adminId(adminId)
            .adminName(adminName)
            .featureKey(featureKey)
            .oldValue(oldValue)
            .newValue(newValue)
            .changeType(changeType)
            .build();
    tokenCostAuditLogRepository.save(auditLog);
  }

  private TokenCostConfigResponse mapToResponse(TokenCostConfig config) {
    return TokenCostConfigResponse.builder()
        .id(config.getId())
        .featureKey(config.getFeatureKey())
        .featureLabel(config.getFeatureLabel())
        .costPerUse(config.getCostPerUse())
        .isActive(config.getIsActive())
        .updatedAt(config.getUpdatedAt())
        .updatedBy(config.getUpdatedBy())
        .build();
  }
}
