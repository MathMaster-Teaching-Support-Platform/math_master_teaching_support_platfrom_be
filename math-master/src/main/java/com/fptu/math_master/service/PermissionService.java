package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.PermissionCreationRequest;
import com.fptu.math_master.dto.request.PermissionUpdateRequest;
import com.fptu.math_master.dto.response.PermissionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

  PermissionResponse createPermission(PermissionCreationRequest request);

  PermissionResponse updatePermission(UUID permissionId, PermissionUpdateRequest request);

  void deletePermission(UUID permissionId);

  PermissionResponse getPermissionById(UUID permissionId);

  PermissionResponse getPermissionByCode(String code);

  List<PermissionResponse> getAllPermissions();

  Page<PermissionResponse> getAllPermissions(Pageable pageable);
}
