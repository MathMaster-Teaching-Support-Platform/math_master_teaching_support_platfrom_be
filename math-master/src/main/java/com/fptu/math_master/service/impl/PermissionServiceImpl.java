package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.PermissionCreationRequest;
import com.fptu.math_master.dto.request.PermissionUpdateRequest;
import com.fptu.math_master.dto.response.PermissionResponse;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.PermissionRepository;
import com.fptu.math_master.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionServiceImpl implements PermissionService {

    PermissionRepository permissionRepository;

    @Override
    @Transactional
    public PermissionResponse createPermission(PermissionCreationRequest request) {
        log.info("Creating permission with code: {}", request.getCode());

        if (permissionRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = Permission.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        permission = permissionRepository.save(permission);

        log.info("Permission created successfully with id: {}", permission.getId());
        return mapToPermissionResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Integer permissionId, PermissionUpdateRequest request) {
        log.info("Updating permission with id: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));

        if (request.getCode() != null) {
            if (!permission.getCode().equals(request.getCode()) && permissionRepository.existsByCode(request.getCode())) {
                throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
            }
            permission.setCode(request.getCode());
        }

        if (request.getName() != null) {
            permission.setName(request.getName());
        }

        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        permission = permissionRepository.save(permission);

        log.info("Permission updated successfully with id: {}", permissionId);
        return mapToPermissionResponse(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Integer permissionId) {
        log.info("Deleting permission with id: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));

        permissionRepository.delete(permission);

        log.info("Permission deleted successfully with id: {}", permissionId);
    }

    @Override
    public PermissionResponse getPermissionById(Integer permissionId) {
        log.info("Getting permission with id: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));

        return mapToPermissionResponse(permission);
    }

    @Override
    public PermissionResponse getPermissionByCode(String code) {
        log.info("Getting permission with code: {}", code);

        Permission permission = permissionRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_EXISTED));

        return mapToPermissionResponse(permission);
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        log.info("Getting all permissions");

        List<Permission> permissions = permissionRepository.findAll();

        return permissions.stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        log.info("Getting all permissions with pagination");

        Page<Permission> permissions = permissionRepository.findAll(pageable);

        return permissions.map(this::mapToPermissionResponse);
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
