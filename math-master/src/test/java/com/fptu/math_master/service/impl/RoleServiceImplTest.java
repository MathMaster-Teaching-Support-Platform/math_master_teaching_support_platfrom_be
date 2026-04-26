package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.RoleCreationRequest;
import com.fptu.math_master.dto.request.RoleUpdateRequest;
import com.fptu.math_master.dto.response.RoleResponse;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.PermissionRepository;
import com.fptu.math_master.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("RoleServiceImpl - Tests")
class RoleServiceImplTest extends BaseUnitTest {

  @InjectMocks private RoleServiceImpl service;
  @Mock private RoleRepository roleRepository;
  @Mock private PermissionRepository permissionRepository;

  private UUID roleId;
  private Permission readPermission;
  private Role role;

  @BeforeEach
  void setUp() {
    roleId = UUID.randomUUID();
    readPermission =
        Permission.builder()
            .code("LESSON_READ")
            .name("Lesson Read")
            .description("Read lesson permission")
            .build();
    readPermission.setId(UUID.randomUUID());
    role = Role.builder().name("CONTENT_EDITOR").permissions(new HashSet<>(Set.of(readPermission))).build();
    role.setId(roleId);
  }

  /** Normal case: Creates role without permissions when permissionCodes is empty. */
  @Test
  void it_should_create_role_without_permissions_when_permission_codes_are_empty() {
    // ===== ARRANGE =====
    RoleCreationRequest request = RoleCreationRequest.builder().name("CONTENT_EDITOR").build();
    when(roleRepository.existsByName("CONTENT_EDITOR")).thenReturn(false);
    when(roleRepository.save(any(Role.class))).thenReturn(role);

    // ===== ACT =====
    RoleResponse result = service.createRole(request);

    // ===== ASSERT =====
    assertNotNull(result);
    assertEquals(roleId, result.getId());
    assertEquals("CONTENT_EDITOR", result.getName());

    // ===== VERIFY =====
    verify(roleRepository, times(1)).existsByName("CONTENT_EDITOR");
    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoMoreInteractions(roleRepository, permissionRepository);
  }

  /** Abnormal case: Throws exception when role name already exists. */
  @Test
  void it_should_throw_exception_when_creating_role_with_duplicate_name() {
    // ===== ARRANGE =====
    RoleCreationRequest request = RoleCreationRequest.builder().name("CONTENT_EDITOR").build();
    when(roleRepository.existsByName("CONTENT_EDITOR")).thenReturn(true);

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.createRole(request));
    assertEquals(ErrorCode.ROLE_ALREADY_EXISTS, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository, times(1)).existsByName("CONTENT_EDITOR");
    verify(roleRepository, never()).save(any());
    verifyNoMoreInteractions(roleRepository, permissionRepository);
  }

  /** Normal case: Updates role name and permissions successfully. */
  @Test
  void it_should_update_role_name_and_permissions_when_request_is_valid() {
    // ===== ARRANGE =====
    RoleUpdateRequest request =
        RoleUpdateRequest.builder().name("COURSE_MANAGER").permissionCodes(Set.of("LESSON_READ")).build();
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.existsByName("COURSE_MANAGER")).thenReturn(false);
    when(permissionRepository.findByCode("LESSON_READ")).thenReturn(Optional.of(readPermission));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // ===== ACT =====
    RoleResponse result = service.updateRole(roleId, request);

    // ===== ASSERT =====
    assertEquals("COURSE_MANAGER", result.getName());
    assertNotNull(result.getPermissions());
    assertEquals(1, result.getPermissions().size());

    // ===== VERIFY =====
    verify(roleRepository).findById(roleId);
    verify(roleRepository).existsByName("COURSE_MANAGER");
    verify(permissionRepository).findByCode("LESSON_READ");
    verify(roleRepository).save(any(Role.class));
  }

  /** Abnormal case: Throws when updating role with duplicate new name. */
  @Test
  void it_should_throw_exception_when_updating_role_with_duplicate_name() {
    // ===== ARRANGE =====
    RoleUpdateRequest request = RoleUpdateRequest.builder().name("ADMIN").build();
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.existsByName("ADMIN")).thenReturn(true);

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.updateRole(roleId, request));
    assertEquals(ErrorCode.ROLE_ALREADY_EXISTS, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findById(roleId);
    verify(roleRepository).existsByName("ADMIN");
    verify(roleRepository, never()).save(any(Role.class));
  }

  /** Normal case: Updates role when name is unchanged and permission codes are null. */
  @Test
  void it_should_update_role_without_name_check_when_name_is_unchanged_and_permission_is_null() {
    // ===== ARRANGE =====
    RoleUpdateRequest request = RoleUpdateRequest.builder().name("CONTENT_EDITOR").build();
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // ===== ACT =====
    RoleResponse result = service.updateRole(roleId, request);

    // ===== ASSERT =====
    assertEquals("CONTENT_EDITOR", result.getName());
    assertEquals(1, result.getPermissions().size());

    // ===== VERIFY =====
    verify(roleRepository).findById(roleId);
    verify(roleRepository, never()).existsByName(any());
    verify(permissionRepository, never()).findByCode(any());
    verify(roleRepository).save(any(Role.class));
  }

  /** Abnormal case: Throws when updating role with unknown permission code. */
  @Test
  void it_should_throw_exception_when_updating_role_with_unknown_permission_code() {
    // ===== ARRANGE =====
    RoleUpdateRequest request = RoleUpdateRequest.builder().permissionCodes(Set.of("UNKNOWN_CODE")).build();
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    when(permissionRepository.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.updateRole(roleId, request));
    assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findById(roleId);
    verify(permissionRepository).findByCode("UNKNOWN_CODE");
    verify(roleRepository, never()).save(any(Role.class));
  }

  /** Abnormal case: Throws exception when update role target does not exist. */
  @Test
  void it_should_throw_exception_when_updating_non_existent_role() {
    // ===== ARRANGE =====
    RoleUpdateRequest request = RoleUpdateRequest.builder().name("COURSE_MANAGER").build();
    when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.updateRole(roleId, request));
    assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findById(roleId);
    verify(roleRepository, never()).save(any());
  }

  /** Normal case: Removes matching permission codes from role permission set. */
  @Test
  void it_should_remove_permissions_from_role_when_codes_are_provided() {
    // ===== ARRANGE =====
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // ===== ACT =====
    RoleResponse result = service.removePermissionsFromRole(roleId, List.of("LESSON_READ"));

    // ===== ASSERT =====
    assertNotNull(result);
    assertNotNull(result.getPermissions());
    assertEquals(0, result.getPermissions().size());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
    verify(roleRepository).save(any(Role.class));
    verifyNoMoreInteractions(permissionRepository);
  }

  /** Normal case: Adds permission to role with null existing set. */
  @Test
  void it_should_add_permission_when_role_permissions_are_null() {
    // ===== ARRANGE =====
    role.setPermissions(null);
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(role));
    when(permissionRepository.findByCode("LESSON_READ")).thenReturn(Optional.of(readPermission));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // ===== ACT =====
    RoleResponse result = service.addPermissionsToRole(roleId, List.of("LESSON_READ"));

    // ===== ASSERT =====
    assertNotNull(result.getPermissions());
    assertEquals(1, result.getPermissions().size());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
    verify(permissionRepository).findByCode("LESSON_READ");
    verify(roleRepository).save(any(Role.class));
  }

  /** Abnormal case: Throws when adding permissions to a non-existent role. */
  @Test
  void it_should_throw_exception_when_adding_permissions_to_non_existent_role() {
    // ===== ARRANGE =====
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception =
        assertThrows(
            AppException.class,
            () -> service.addPermissionsToRole(roleId, List.of("LESSON_READ")));
    assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
    verify(roleRepository, never()).save(any(Role.class));
  }

  /** Abnormal case: Throws when adding a permission code that does not exist. */
  @Test
  void it_should_throw_exception_when_adding_non_existent_permission_to_role() {
    // ===== ARRANGE =====
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(role));
    when(permissionRepository.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception =
        assertThrows(
            AppException.class,
            () -> service.addPermissionsToRole(roleId, List.of("UNKNOWN_CODE")));
    assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
    verify(permissionRepository).findByCode("UNKNOWN_CODE");
    verify(roleRepository, never()).save(any(Role.class));
  }

  /** Normal case: Removes permissions when role has null permission set. */
  @Test
  void it_should_keep_role_unchanged_when_removing_permissions_from_null_permission_set() {
    // ===== ARRANGE =====
    role.setPermissions(null);
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // ===== ACT =====
    RoleResponse result = service.removePermissionsFromRole(roleId, List.of("LESSON_READ"));

    // ===== ASSERT =====
    assertNull(result.getPermissions());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
    verify(roleRepository).save(any(Role.class));
  }

  /** Normal case: Gets all roles and maps permissions including null permission set. */
  @Test
  void it_should_get_all_roles_and_map_permission_set() {
    // ===== ARRANGE =====
    Role roleWithoutPermissions = Role.builder().name("VIEWER").permissions(null).build();
    roleWithoutPermissions.setId(UUID.randomUUID());
    when(roleRepository.findAll()).thenReturn(List.of(role, roleWithoutPermissions));

    // ===== ACT =====
    List<RoleResponse> result = service.getAllRoles();

    // ===== ASSERT =====
    assertEquals(2, result.size());
    assertEquals("CONTENT_EDITOR", result.get(0).getName());
    assertNull(result.get(1).getPermissions());

    // ===== VERIFY =====
    verify(roleRepository).findAll();
  }

  /** Abnormal case: Throws when role by id is not found. */
  @Test
  void it_should_throw_exception_when_get_role_by_id_not_found() {
    // ===== ARRANGE =====
    when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.getRoleById(roleId));
    assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).findByIdWithPermissions(roleId);
  }

  /** Abnormal case: Throws when creating role with unknown permission code. */
  @Test
  void it_should_throw_exception_when_create_role_with_unknown_permission_code() {
    // ===== ARRANGE =====
    RoleCreationRequest request =
        RoleCreationRequest.builder().name("CONTENT_EDITOR").permissionCodes(Set.of("UNKNOWN_CODE")).build();
    when(roleRepository.existsByName("CONTENT_EDITOR")).thenReturn(false);
    when(permissionRepository.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(AppException.class, () -> service.createRole(request));
    assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, exception.getErrorCode());

    // ===== VERIFY =====
    verify(roleRepository).existsByName("CONTENT_EDITOR");
    verify(permissionRepository).findByCode("UNKNOWN_CODE");
    verify(roleRepository, never()).save(any(Role.class));
  }
}
