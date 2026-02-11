package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.RoleCreationRequest;
import com.fptu.math_master.dto.request.RoleUpdateRequest;
import com.fptu.math_master.dto.response.PermissionResponse;
import com.fptu.math_master.dto.response.RoleResponse;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.PermissionRepository;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

  RoleRepository roleRepository;
  PermissionRepository permissionRepository;

  @Override
  @Transactional
  public RoleResponse createRole(RoleCreationRequest request) {
    log.info("Creating role with name: {}", request.getName());

    if (roleRepository.existsByName(request.getName())) {
      throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
    }

    Role role = Role.builder()
      .name(request.getName())
      .build();

    // Set permissions if provided
    if (request.getPermissionCodes() != null && !request.getPermissionCodes().isEmpty()) {
      Set<Permission> permissions = new HashSet<>();
      for (String code : request.getPermissionCodes()) {
        Permission permission = permissionRepository.findByCode(code)
          .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));
        permissions.add(permission);
      }
      role.setPermissions(permissions);
    }

    role = roleRepository.save(role);

    log.info("Role created successfully with id: {}", role.getId());
    return mapToRoleResponse(role);
  }

  @Override
  @Transactional
  public RoleResponse updateRole(UUID roleId, RoleUpdateRequest request) {
    log.info("Updating role with id: {}", roleId);

    Role role = roleRepository.findById(roleId)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    // Update name if provided
    if (request.getName() != null) {
      // Check if new name already exists
      if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
        throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
      }
      role.setName(request.getName());
    }

    // Update permissions if provided
    if (request.getPermissionCodes() != null) {
      Set<Permission> permissions = new HashSet<>();
      for (String code : request.getPermissionCodes()) {
        Permission permission = permissionRepository.findByCode(code)
          .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));
        permissions.add(permission);
      }
      role.setPermissions(permissions);
    }

    role = roleRepository.save(role);

    log.info("Role updated successfully with id: {}", roleId);
    return mapToRoleResponse(role);
  }

  @Override
  @Transactional
  public void deleteRole(UUID roleId) {
    log.info("Deleting role with id: {}", roleId);

    Role role = roleRepository.findById(roleId)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    roleRepository.delete(role);

    log.info("Role deleted successfully with id: {}", roleId);
  }

  @Override
  public RoleResponse getRoleById(UUID roleId) {
    log.info("Getting role with id: {}", roleId);

    Role role = roleRepository.findByIdWithPermissions(roleId)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    return mapToRoleResponse(role);
  }

  @Override
  public RoleResponse getRoleByName(String name) {
    log.info("Getting role with name: {}", name);

    Role role = roleRepository.findByName(name)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    return mapToRoleResponse(role);
  }

  @Override
  public List<RoleResponse> getAllRoles() {
    log.info("Getting all roles");

    List<Role> roles = roleRepository.findAll();

    return roles.stream()
      .map(this::mapToRoleResponse)
      .collect(Collectors.toList());
  }

  @Override
  public Page<RoleResponse> getAllRoles(Pageable pageable) {
    log.info("Getting all roles with pagination");

    Page<Role> roles = roleRepository.findAll(pageable);

    return roles.map(this::mapToRoleResponse);
  }

  @Override
  @Transactional
  public RoleResponse addPermissionsToRole(UUID roleId, List<String> permissionCodes) {
    log.info("Adding permissions to role with id: {}", roleId);

    Role role = roleRepository.findByIdWithPermissions(roleId)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    if (role.getPermissions() == null) {
      role.setPermissions(new HashSet<>());
    }

    for (String code : permissionCodes) {
      Permission permission = permissionRepository.findByCode(code)
        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));
      role.getPermissions().add(permission);
    }

    role = roleRepository.save(role);

    log.info("Permissions added successfully to role with id: {}", roleId);
    return mapToRoleResponse(role);
  }

  @Override
  @Transactional
  public RoleResponse removePermissionsFromRole(UUID roleId, List<String> permissionCodes) {
    log.info("Removing permissions from role with id: {}", roleId);

    Role role = roleRepository.findByIdWithPermissions(roleId)
      .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    if (role.getPermissions() != null) {
      for (String code : permissionCodes) {
        role.getPermissions().removeIf(permission -> permission.getCode().equals(code));
      }
    }

    role = roleRepository.save(role);

    log.info("Permissions removed successfully from role with id: {}", roleId);
    return mapToRoleResponse(role);
  }

  private RoleResponse mapToRoleResponse(Role role) {
    Set<PermissionResponse> permissions = null;
    if (role.getPermissions() != null) {
      permissions = role.getPermissions().stream()
        .map(this::mapToPermissionResponse)
        .collect(Collectors.toSet());
    }

    return RoleResponse.builder()
      .id(role.getId())
      .name(role.getName())
      .permissions(permissions)
      .build();
  }

  private PermissionResponse mapToPermissionResponse(Permission permission) {
    return PermissionResponse.builder()
      .id(permission.getId())
      .code(permission.getCode())
      .name(permission.getName())
      .description(permission.getDescription())
      .build();
  }
}

