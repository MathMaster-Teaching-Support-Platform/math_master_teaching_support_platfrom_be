package com.fptu.math_master.service;

import java.util.List;
import java.util.UUID;

import com.fptu.math_master.dto.request.UpdateSystemConfigRequest;
import com.fptu.math_master.dto.response.SystemConfigResponse;

public interface SystemConfigService {

    /** Retrieve a single config by key. Throws {@code AppException} if not found. */
    SystemConfigResponse getByKey(String key);

    /** List all active system configs (admin use). */
    List<SystemConfigResponse> listAll();

    /** Update the value of an existing config entry. */
    SystemConfigResponse update(String key, UpdateSystemConfigRequest request, UUID updatedBy);
}
