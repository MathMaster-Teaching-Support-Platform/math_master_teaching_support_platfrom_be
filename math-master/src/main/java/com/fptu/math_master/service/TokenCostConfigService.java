package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.TokenCostConfigRequest;
import com.fptu.math_master.dto.response.TokenCostConfigResponse;
import com.fptu.math_master.entity.TokenCostAuditLog;
import java.util.List;
import java.util.UUID;

public interface TokenCostConfigService {
  List<TokenCostConfigResponse> getAllConfigs();

  TokenCostConfigResponse updateConfig(UUID id, TokenCostConfigRequest request);

  Integer getCostPerUse(String featureKey);

  List<TokenCostAuditLog> getAuditLogs();
}
