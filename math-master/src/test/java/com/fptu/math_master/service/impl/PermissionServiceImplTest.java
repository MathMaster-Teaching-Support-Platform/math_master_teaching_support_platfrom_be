package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.PermissionCreationRequest;
import com.fptu.math_master.dto.request.PermissionUpdateRequest;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.PermissionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("PermissionServiceImpl - Tests")
class PermissionServiceImplTest extends BaseUnitTest {

  @InjectMocks private PermissionServiceImpl permissionService;

  @Mock private PermissionRepository permissionRepository;

  private static final UUID PERMISSION_ID = UUID.fromString("96000000-0000-0000-0000-000000000001");

  private Permission buildPermission(UUID id, String code, String name, String description) {
    Permission permission = new Permission();
    permission.setId(id);
    permission.setCode(code);
    permission.setName(name);
    permission.setDescription(description);
    return permission;
  }

  @Nested
  @DisplayName("createPermission()")
  class CreatePermissionTests {

    @Test
    void it_should_create_permission_when_code_not_exists() {
      // ===== ARRANGE =====
      PermissionCreationRequest request =
          PermissionCreationRequest.builder()
              .code("COURSE_READ")
              .name("Read course")
              .description("Can read course")
              .build();
      Permission saved =
          buildPermission(PERMISSION_ID, "COURSE_READ", "Read course", "Can read course");
      when(permissionRepository.existsByCode("COURSE_READ")).thenReturn(false);
      when(permissionRepository.save(any(Permission.class))).thenReturn(saved);

      // ===== ACT =====
      var response = permissionService.createPermission(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(PERMISSION_ID, response.getId()),
          () -> assertEquals("COURSE_READ", response.getCode()),
          () -> assertEquals("Read course", response.getName()));
    }

    @Test
    void it_should_throw_already_exists_when_create_with_duplicate_code() {
      // ===== ARRANGE =====
      PermissionCreationRequest request =
          PermissionCreationRequest.builder().code("COURSE_READ").name("Read course").build();
      when(permissionRepository.existsByCode("COURSE_READ")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> permissionService.createPermission(request));
      assertEquals(ErrorCode.PERMISSION_ALREADY_EXISTS, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updatePermission()")
  class UpdatePermissionTests {

    @Test
    void it_should_update_code_name_description_when_request_has_non_null_values() {
      // ===== ARRANGE =====
      Permission existing =
          buildPermission(PERMISSION_ID, "COURSE_READ", "Read course", "Old description");
      PermissionUpdateRequest request =
          PermissionUpdateRequest.builder()
              .code("COURSE_WRITE")
              .name("Write course")
              .description("New description")
              .build();
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));
      when(permissionRepository.existsByCode("COURSE_WRITE")).thenReturn(false);
      when(permissionRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      var response = permissionService.updatePermission(PERMISSION_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("COURSE_WRITE", response.getCode()),
          () -> assertEquals("Write course", response.getName()),
          () -> assertEquals("New description", response.getDescription()));
    }

    @Test
    void it_should_skip_code_uniqueness_check_when_code_is_null_or_unchanged() {
      // ===== ARRANGE =====
      Permission existing =
          buildPermission(PERMISSION_ID, "COURSE_READ", "Read course", "Old description");
      PermissionUpdateRequest nullCodeRequest =
          PermissionUpdateRequest.builder().name("Updated name").build();
      PermissionUpdateRequest sameCodeRequest =
          PermissionUpdateRequest.builder().code("COURSE_READ").description("Desc updated").build();
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));
      when(permissionRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      var first = permissionService.updatePermission(PERMISSION_ID, nullCodeRequest);
      var second = permissionService.updatePermission(PERMISSION_ID, sameCodeRequest);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Updated name", first.getName()),
          () -> assertEquals("COURSE_READ", second.getCode()),
          () -> assertEquals("Desc updated", second.getDescription()));
      verify(permissionRepository, never()).existsByCode(any());
    }

    @Test
    void it_should_throw_not_existed_when_update_permission_not_found() {
      // ===== ARRANGE =====
      PermissionUpdateRequest request = PermissionUpdateRequest.builder().name("x").build();
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> permissionService.updatePermission(PERMISSION_ID, request));
      assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_already_exists_when_update_to_different_duplicate_code() {
      // ===== ARRANGE =====
      Permission existing = buildPermission(PERMISSION_ID, "COURSE_READ", "Read", "Desc");
      PermissionUpdateRequest request = PermissionUpdateRequest.builder().code("COURSE_WRITE").build();
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));
      when(permissionRepository.existsByCode("COURSE_WRITE")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> permissionService.updatePermission(PERMISSION_ID, request));
      assertEquals(ErrorCode.PERMISSION_ALREADY_EXISTS, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("delete and get methods")
  class DeleteAndGetTests {

    @Test
    void it_should_delete_permission_when_exists() {
      // ===== ARRANGE =====
      Permission existing = buildPermission(PERMISSION_ID, "COURSE_READ", "Read", "Desc");
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));

      // ===== ACT =====
      permissionService.deletePermission(PERMISSION_ID);

      // ===== ASSERT =====
      verify(permissionRepository, times(1)).delete(existing);
    }

    @Test
    void it_should_throw_not_existed_when_delete_permission_not_found() {
      // ===== ARRANGE =====
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> permissionService.deletePermission(PERMISSION_ID));
      assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void it_should_get_permission_by_id_and_code() {
      // ===== ARRANGE =====
      Permission existing = buildPermission(PERMISSION_ID, "COURSE_READ", "Read", "Desc");
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));
      when(permissionRepository.findByCode("COURSE_READ")).thenReturn(Optional.of(existing));

      // ===== ACT =====
      var byId = permissionService.getPermissionById(PERMISSION_ID);
      var byCode = permissionService.getPermissionByCode("COURSE_READ");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(PERMISSION_ID, byId.getId()),
          () -> assertEquals("COURSE_READ", byCode.getCode()));
    }

    @Test
    void it_should_throw_not_existed_when_get_by_id_or_code_not_found() {
      // ===== ARRANGE =====
      when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());
      when(permissionRepository.findByCode("COURSE_READ")).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException byId =
          assertThrows(AppException.class, () -> permissionService.getPermissionById(PERMISSION_ID));
      AppException byCode =
          assertThrows(AppException.class, () -> permissionService.getPermissionByCode("COURSE_READ"));
      assertAll(
          () -> assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, byId.getErrorCode()),
          () -> assertEquals(ErrorCode.PERMISSION_NOT_EXISTED, byCode.getErrorCode()));
    }

    @Test
    void it_should_get_all_permissions_in_list_and_page_modes() {
      // ===== ARRANGE =====
      Permission p1 = buildPermission(PERMISSION_ID, "COURSE_READ", "Read", "Desc");
      when(permissionRepository.findAll()).thenReturn(List.of(p1));
      when(permissionRepository.findAll(PageRequest.of(0, 10)))
          .thenReturn(new PageImpl<>(List.of(p1), PageRequest.of(0, 10), 1));

      // ===== ACT =====
      var list = permissionService.getAllPermissions();
      var page = permissionService.getAllPermissions(PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, list.size()), () -> assertEquals(1, page.getTotalElements()));
    }
  }
}
