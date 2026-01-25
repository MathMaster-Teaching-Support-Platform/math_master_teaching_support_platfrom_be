package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.RoleCreationRequest;
import com.fptu.math_master.dto.request.RoleUpdateRequest;
import com.fptu.math_master.dto.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(RoleCreationRequest request);

    RoleResponse updateRole(Integer roleId, RoleUpdateRequest request);

    void deleteRole(Integer roleId);

    RoleResponse getRoleById(Integer roleId);

    RoleResponse getRoleByName(String name);

    List<RoleResponse> getAllRoles();

    Page<RoleResponse> getAllRoles(Pageable pageable);

    RoleResponse addPermissionsToRole(Integer roleId, List<String> permissionCodes);

    RoleResponse removePermissionsFromRole(Integer roleId, List<String> permissionCodes);
}

