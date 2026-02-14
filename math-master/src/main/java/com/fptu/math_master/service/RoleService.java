package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.RoleCreationRequest;
import com.fptu.math_master.dto.request.RoleUpdateRequest;
import com.fptu.math_master.dto.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RoleService {

  RoleResponse createRole(RoleCreationRequest request);

  RoleResponse updateRole(UUID roleId, RoleUpdateRequest request);

  void deleteRole(UUID roleId);

  RoleResponse getRoleById(UUID roleId);

  RoleResponse getRoleByName(String name);

  List<RoleResponse> getAllRoles();

  Page<RoleResponse> getAllRoles(Pageable pageable);

  RoleResponse addPermissionsToRole(UUID roleId, List<String> permissionCodes);

  RoleResponse removePermissionsFromRole(UUID roleId, List<String> permissionCodes);
}

