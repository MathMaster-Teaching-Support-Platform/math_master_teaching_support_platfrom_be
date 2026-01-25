package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.PermissionCreationRequest;
import com.fptu.math_master.dto.request.PermissionUpdateRequest;
import com.fptu.math_master.dto.response.PermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PermissionService {

    PermissionResponse createPermission(PermissionCreationRequest request);

    PermissionResponse updatePermission(Integer permissionId, PermissionUpdateRequest request);

    void deletePermission(Integer permissionId);

    PermissionResponse getPermissionById(Integer permissionId);

    PermissionResponse getPermissionByCode(String code);

    List<PermissionResponse> getAllPermissions();

    Page<PermissionResponse> getAllPermissions(Pageable pageable);
}

