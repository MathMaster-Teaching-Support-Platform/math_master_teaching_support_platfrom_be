package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.ChangePasswordRequest;
import com.fptu.math_master.dto.request.UserCreationRequest;
import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.dto.request.UserUpdateRequest;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@DisplayName("UserServiceImpl - Tests")
class UserServiceImplTest extends BaseUnitTest {

  @InjectMocks private UserServiceImpl userService;

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  private UUID userId;
  private User baseUser;
  private Role studentRole;
  private Role teacherRole;

  @BeforeEach
  void setUp() {
    userId = UUID.fromString("12345678-1234-1234-1234-123456789012");
    studentRole = buildRole("STUDENT");
    teacherRole = buildRole("TEACHER");
    baseUser = buildUser(userId, "ngo.minh.hieu", "ngo.minh.hieu@fptu.edu.vn", Status.ACTIVE, Set.of(studentRole));
  }

  private Role buildRole(String roleName) {
    Role role = Role.builder().name(roleName).build();
    role.setId(UUID.randomUUID());
    return role;
  }

  private User buildUser(UUID id, String userName, String email, Status status, Set<Role> roles) {
    User user =
        User.builder()
            .userName(userName)
            .password("hashed-password")
            .fullName("Ngô Minh Hiếu")
            .email(email)
            .phoneNumber("0912345678")
            .gender(Gender.MALE)
            .avatar("https://cdn.mathmaster.vn/avatar/hieu.png")
            .dob(LocalDate.of(2003, 3, 12))
            .code("HSMATH2026")
            .status(status)
            .roles(roles)
            .build();
    user.setId(id);
    user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    user.setUpdatedAt(Instant.parse("2026-04-01T00:00:00Z"));
    user.setCreatedByName("System");
    user.setUpdatedByName("Admin");
    return user;
  }

  @Nested
  @DisplayName("createUser()")
  class CreateUserTests {

    @Test
    void it_should_create_user_with_roles_when_input_is_valid() {
      // ===== ARRANGE =====
      UserCreationRequest request =
          UserCreationRequest.builder()
              .userName("pham.hoang.an")
              .password("Abc@1234")
              .fullName("Phạm Hoàng An")
              .email("pham.hoang.an@fptu.edu.vn")
              .phoneNumber("0987654321")
              .gender(Gender.FEMALE)
              .avatar("https://cdn.mathmaster.vn/avatar/an.png")
              .dob(LocalDate.of(2004, 5, 10))
              .code("ANMATH")
              .roles(Set.of("STUDENT", "TEACHER"))
              .build();
      User saved = buildUser(userId, request.getUserName(), request.getEmail(), Status.ACTIVE, Set.of(studentRole, teacherRole));
      when(userRepository.existsByUserName(request.getUserName())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
      when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
      when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
      when(userRepository.save(any(User.class))).thenReturn(saved);

      // ===== ACT =====
      UserResponse result = userService.createUser(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(userId, result.getId()),
          () -> assertEquals("pham.hoang.an", result.getUserName()),
          () -> assertEquals(Status.ACTIVE, result.getStatus()),
          () -> assertNotNull(result.getRoles()),
          () -> assertEquals(2, result.getRoles().size()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName(request.getUserName());
      verify(userRepository, times(1)).existsByEmail(request.getEmail());
      verify(passwordEncoder, times(1)).encode(request.getPassword());
      verify(roleRepository, times(1)).findByName("STUDENT");
      verify(roleRepository, times(1)).findByName("TEACHER");
      verify(userRepository, times(1)).save(any(User.class));
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_username_already_exists() {
      // ===== ARRANGE =====
      UserCreationRequest request = UserCreationRequest.builder().userName("ngo.minh.hieu").build();
      when(userRepository.existsByUserName("ngo.minh.hieu")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.createUser(request));
      assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName("ngo.minh.hieu");
      verify(userRepository, never()).existsByEmail(any());
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_email_already_exists() {
      // ===== ARRANGE =====
      UserCreationRequest request =
          UserCreationRequest.builder().userName("new.user").email("ngo.minh.hieu@fptu.edu.vn").build();
      when(userRepository.existsByUserName("new.user")).thenReturn(false);
      when(userRepository.existsByEmail("ngo.minh.hieu@fptu.edu.vn")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.createUser(request));
      assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName("new.user");
      verify(userRepository, times(1)).existsByEmail("ngo.minh.hieu@fptu.edu.vn");
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_create_user_without_roles_when_request_roles_is_empty() {
      // ===== ARRANGE =====
      UserCreationRequest request =
          UserCreationRequest.builder()
              .userName("le.ngoc.minh")
              .password("Abc@1234")
              .fullName("Lê Ngọc Minh")
              .email("le.ngoc.minh@fptu.edu.vn")
              .roles(Set.of())
              .build();
      User saved = buildUser(userId, request.getUserName(), request.getEmail(), Status.ACTIVE, null);
      when(userRepository.existsByUserName(request.getUserName())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
      when(userRepository.save(any(User.class))).thenReturn(saved);

      // ===== ACT =====
      UserResponse result = userService.createUser(request);

      // ===== ASSERT =====
      assertNull(result.getRoles());

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName(request.getUserName());
      verify(userRepository, times(1)).existsByEmail(request.getEmail());
      verify(passwordEncoder, times(1)).encode(request.getPassword());
      verify(roleRepository, never()).findByName(any());
      verify(userRepository, times(1)).save(any(User.class));
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }
  }

  @Nested
  @DisplayName("updateUser()")
  class UpdateUserTests {

    @Test
    void it_should_update_fields_and_roles_when_request_contains_values() {
      // ===== ARRANGE =====
      UserUpdateRequest request =
          UserUpdateRequest.builder()
              .password("New@12345")
              .fullName("Ngô Minh Hiếu Updated")
              .email("hieu.updated@fptu.edu.vn")
              .phoneNumber("0977777777")
              .gender(Gender.OTHER)
              .avatar("https://cdn.mathmaster.vn/avatar/hieu-new.png")
              .dob(LocalDate.of(2002, 11, 9))
              .code("HIEU-UPD")
              .status(Status.INACTIVE)
              .roles(Set.of("TEACHER"))
              .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.existsByEmail("hieu.updated@fptu.edu.vn")).thenReturn(false);
      when(passwordEncoder.encode("New@12345")).thenReturn("new-hashed");
      when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      UserResponse result = userService.updateUser(userId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(userId, result.getId()),
          () -> assertEquals("hieu.updated@fptu.edu.vn", result.getEmail()),
          () -> assertEquals(Status.INACTIVE, result.getStatus()),
          () -> assertEquals(1, result.getRoles().size()),
          () -> assertEquals("TEACHER", result.getRoles().iterator().next()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).existsByEmail("hieu.updated@fptu.edu.vn");
      verify(passwordEncoder, times(1)).encode("New@12345");
      verify(roleRepository, times(1)).findByName("TEACHER");
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_new_email_already_exists() {
      // ===== ARRANGE =====
      UserUpdateRequest request = UserUpdateRequest.builder().email("dup@fptu.edu.vn").build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.existsByEmail("dup@fptu.edu.vn")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.updateUser(userId, request));
      assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).existsByEmail("dup@fptu.edu.vn");
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_requested_role_does_not_exist() {
      // ===== ARRANGE =====
      UserUpdateRequest request = UserUpdateRequest.builder().roles(Set.of("ADMIN")).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.updateUser(userId, request));
      assertEquals(ErrorCode.ROLE_NOT_EXISTED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(roleRepository, times(1)).findByName("ADMIN");
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_update_user_when_email_is_unchanged_without_email_existence_check() {
      // ===== ARRANGE =====
      UserUpdateRequest request = UserUpdateRequest.builder().email(baseUser.getEmail()).build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      UserResponse result = userService.updateUser(userId, request);

      // ===== ASSERT =====
      assertEquals(baseUser.getEmail(), result.getEmail());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).existsByEmail(any());
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }
  }

  @Nested
  @DisplayName("status management")
  class StatusManagementTests {

    @Test
    void it_should_soft_delete_user_when_delete_user_is_called() {
      // ===== ARRANGE =====
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));

      // ===== ACT =====
      userService.deleteUser(userId);

      // ===== ASSERT =====
      assertEquals(Status.DELETED, baseUser.getStatus());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_banning_already_banned_user() {
      // ===== ARRANGE =====
      baseUser.setStatus(Status.BANNED);
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.banUser(userId, "Spam nội dung"));
      assertEquals(ErrorCode.USER_ALREADY_BANNED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_ban_user_when_status_is_not_banned() {
      // ===== ARRANGE =====
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      UserResponse result = userService.banUser(userId, "Vi phạm điều khoản cộng đồng");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Status.BANNED, result.getStatus()),
          () -> assertEquals("Vi phạm điều khoản cộng đồng", result.getBanReason()),
          () -> assertNotNull(result.getBanDate()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_unbanning_non_banned_user() {
      // ===== ARRANGE =====
      baseUser.setStatus(Status.ACTIVE);
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.unbanUser(userId));
      assertEquals(ErrorCode.USER_NOT_BANNED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_restore_user_to_active_when_unban_user_succeeds() {
      // ===== ARRANGE =====
      baseUser.setStatus(Status.BANNED);
      baseUser.setBanReason("old reason");
      baseUser.setBanDate(Instant.parse("2026-03-01T00:00:00Z"));
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      UserResponse result = userService.unbanUser(userId);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Status.ACTIVE, result.getStatus()),
          () -> assertNull(result.getBanReason()),
          () -> assertNull(result.getBanDate()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_disabling_inactive_user() {
      // ===== ARRANGE =====
      baseUser.setStatus(Status.INACTIVE);
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> userService.disableUser(userId));
      assertEquals(ErrorCode.USER_ALREADY_DISABLED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_enabling_active_or_deleted_user() {
      // ===== ARRANGE =====
      User activeUser = buildUser(userId, "active.user", "active.user@fptu.edu.vn", Status.ACTIVE, Set.of(studentRole));
      when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

      // ===== ACT & ASSERT =====
      AppException activeEx = assertThrows(AppException.class, () -> userService.enableUser(userId));
      assertEquals(ErrorCode.USER_ALREADY_ENABLED, activeEx.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository);

      Mockito.reset(userRepository, roleRepository, passwordEncoder);
      User deletedUser = buildUser(userId, "del.user", "del.user@fptu.edu.vn", Status.DELETED, Set.of(studentRole));
      when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

      AppException deletedEx = assertThrows(AppException.class, () -> userService.enableUser(userId));
      assertEquals(ErrorCode.USER_NOT_EXISTED, deletedEx.getErrorCode());

      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_disable_and_enable_user_when_state_transition_is_valid() {
      // ===== ARRANGE =====
      User inactive = buildUser(userId, "inactive.user", "inactive.user@fptu.edu.vn", Status.INACTIVE, Set.of(studentRole));
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      UserResponse disabled = userService.disableUser(userId);

      // ===== ASSERT =====
      assertEquals(Status.INACTIVE, disabled.getStatus());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository);

      Mockito.reset(userRepository, roleRepository, passwordEncoder);
      when(userRepository.findById(userId)).thenReturn(Optional.of(inactive));
      when(userRepository.save(inactive)).thenReturn(inactive);

      UserResponse enabled = userService.enableUser(userId);
      assertEquals(Status.ACTIVE, enabled.getStatus());

      verify(userRepository, times(1)).findById(userId);
      verify(userRepository, times(1)).save(inactive);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }
  }

  @Nested
  @DisplayName("query and profile methods")
  class QueryAndProfileTests {

    @Test
    void it_should_return_user_by_id_or_throw_when_not_found() {
      // ===== ARRANGE =====
      when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(baseUser));

      // ===== ACT =====
      UserResponse result = userService.getUserById(userId);

      // ===== ASSERT =====
      assertEquals(userId, result.getId());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByIdWithRoles(userId);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_map_users_for_get_all_and_paginated_and_recent_users() {
      // ===== ARRANGE =====
      User second = buildUser(
          UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
          "tran.linh.chi",
          "tran.linh.chi@fptu.edu.vn",
          Status.INACTIVE,
          null);
      when(userRepository.findAll()).thenReturn(List.of(baseUser, second));
      Pageable pageable = PageRequest.of(0, 2);
      Page<User> page = new PageImpl<>(List.of(baseUser, second), pageable, 2);
      when(userRepository.findAll(pageable)).thenReturn(page);
      when(userRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

      // ===== ACT =====
      List<UserResponse> listResult = userService.getAllUsers();
      Page<UserResponse> pageResult = userService.getAllUsers(pageable);
      Page<UserResponse> recentResult = userService.getRecentUsers(pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(2, listResult.size()),
          () -> assertEquals(2, pageResult.getTotalElements()),
          () -> assertEquals(2, recentResult.getTotalElements()),
          () -> assertNull(listResult.get(1).getRoles()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll();
      verify(userRepository, times(1)).findAll(pageable);
      verify(userRepository, times(1)).findAllByOrderByCreatedAtDesc(pageable);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_get_and_update_my_info_using_security_context() {
      // ===== ARRANGE =====
      UserUpdateRequest request =
          UserUpdateRequest.builder()
              .password("MyNew@123")
              .fullName("Current User Updated")
              .phoneNumber("0909090909")
              .gender(Gender.FEMALE)
              .avatar("https://cdn.mathmaster.vn/avatar/current.png")
              .dob(LocalDate.of(2004, 1, 1))
              .build();
      when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(baseUser));
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(passwordEncoder.encode("MyNew@123")).thenReturn("encoded-new");
      when(userRepository.save(baseUser)).thenReturn(baseUser);

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        UserResponse myInfo = userService.getMyInfo();
        UserResponse updated = userService.updateMyInfo(request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(userId, myInfo.getId()),
            () -> assertEquals("Current User Updated", updated.getFullName()),
            () -> assertEquals(Gender.FEMALE, updated.getGender()));
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByIdWithRoles(userId);
      verify(userRepository, times(1)).findById(userId);
      verify(passwordEncoder, times(1)).encode("MyNew@123");
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_delegate_search_users_with_specification_and_pageable() {
      // ===== ARRANGE =====
      UserSearchRequest request = UserSearchRequest.builder().keyword("hieu").build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> page = new PageImpl<>(List.of(baseUser), pageable, 1);
      when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
          .thenReturn(page);

      // ===== ACT =====
      Page<UserResponse> result = userService.searchUsers(request, pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getTotalElements());

      // ===== VERIFY =====
      verify(userRepository, times(1))
          .findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), eq(pageable));
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }
  }

  @Nested
  @DisplayName("changePassword()")
  class ChangePasswordTests {

    @Test
    void it_should_throw_exception_when_current_password_is_incorrect() {
      // ===== ARRANGE =====
      ChangePasswordRequest request =
          ChangePasswordRequest.builder()
              .currentPassword("WrongCurrent@1")
              .newPassword("BrandNew@1")
              .confirmPassword("BrandNew@1")
              .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(passwordEncoder.matches("WrongCurrent@1", "hashed-password")).thenReturn(false);

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        AppException ex = assertThrows(AppException.class, () -> userService.changePassword(request));
        assertEquals(ErrorCode.INCORRECT_PASSWORD, ex.getErrorCode());
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(passwordEncoder, times(1)).matches("WrongCurrent@1", "hashed-password");
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_throw_exception_when_new_password_and_confirm_password_do_not_match() {
      // ===== ARRANGE =====
      ChangePasswordRequest request =
          ChangePasswordRequest.builder()
              .currentPassword("Old@1234")
              .newPassword("BrandNew@1")
              .confirmPassword("Mismatch@1")
              .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(passwordEncoder.matches("Old@1234", "hashed-password")).thenReturn(true);

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        AppException ex = assertThrows(AppException.class, () -> userService.changePassword(request));
        assertEquals(ErrorCode.PASSWORD_MISMATCH, ex.getErrorCode());
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(passwordEncoder, times(1)).matches("Old@1234", "hashed-password");
      verify(passwordEncoder, never()).encode(any());
      verify(userRepository, never()).save(any());
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void it_should_change_password_when_current_password_is_valid_and_confirm_matches() {
      // ===== ARRANGE =====
      ChangePasswordRequest request =
          ChangePasswordRequest.builder()
              .currentPassword("Old@1234")
              .newPassword("BrandNew@1")
              .confirmPassword("BrandNew@1")
              .build();
      when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
      when(passwordEncoder.matches("Old@1234", "hashed-password")).thenReturn(true);
      when(passwordEncoder.encode("BrandNew@1")).thenReturn("encoded-brand-new");

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        userService.changePassword(request);
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(userId);
      verify(passwordEncoder, times(1)).matches("Old@1234", "hashed-password");
      verify(passwordEncoder, times(1)).encode("BrandNew@1");
      verify(userRepository, times(1)).save(baseUser);
      verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder);
    }
  }
}
