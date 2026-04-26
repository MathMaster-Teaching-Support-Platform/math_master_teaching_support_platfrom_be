package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.InitProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.ForgotPasswordRequest;
import com.fptu.math_master.dto.request.GoogleAuthRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.request.ResetPasswordRequest;
import com.fptu.math_master.dto.request.RoleSelectionRequest;
import com.fptu.math_master.dto.request.UserRegistrationRequest;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.fptu.math_master.dto.response.UserRegisterResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.InvalidatedTokenRepository;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("AuthenticationServiceImpl - Tests")
class AuthenticationServiceImplTest extends BaseUnitTest {

  @InjectMocks private AuthenticationServiceImpl authenticationService;

  @Mock private UserRepository userRepository;
  @Mock private InvalidatedTokenRepository invalidatedTokenRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private InitProperties initProperties;
  @Mock private EmailService emailService;

  private static final String SIGNER_KEY = "12345678901234567890123456789012";
  private static final long VALID_DURATION_SECONDS = 3600L;
  private static final long REFRESHABLE_DURATION_SECONDS = 7200L;
  private static final String FRONTEND_URL = "https://mathmaster.edu.vn";

  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @BeforeEach
  void setUp() {
    authenticationService.SIGNER_KEY = SIGNER_KEY;
    authenticationService.VALID_DURATION = VALID_DURATION_SECONDS;
    authenticationService.REFRESHABLE_DURATION = REFRESHABLE_DURATION_SECONDS;
    authenticationService.FRONTEND_URL = FRONTEND_URL;
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private String encodePassword(String rawPassword) {
    return new BCryptPasswordEncoder(10).encode(rawPassword);
  }

  private User buildUser(UUID id, String userName, String email, String password, Status status) {
    User user = new User();
    user.setId(id);
    user.setUserName(userName);
    user.setEmail(email);
    user.setPassword(password);
    user.setStatus(status);
    return user;
  }

  private Role buildRole(String roleName, List<String> permissionCodes) {
    Role role = new Role();
    role.setName(roleName);
    if (permissionCodes != null) {
      Set<Permission> permissions = new HashSet<>();
      for (String permissionCode : permissionCodes) {
        Permission permission = new Permission();
        permission.setCode(permissionCode);
        permission.setName(permissionCode + " permission");
        permissions.add(permission);
      }
      role.setPermissions(permissions);
    }
    return role;
  }

  private String createSignedToken(
      UUID userId, String jwtId, Instant issuedAt, Instant expiredAt, String purpose)
      throws JOSEException {
    JWTClaimsSet.Builder claimsBuilder =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issuer("school.edu")
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiredAt))
            .jwtID(jwtId);

    if (purpose != null) {
      claimsBuilder.claim("purpose", purpose);
    }

    var jwsObject = new com.nimbusds.jose.JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(claimsBuilder.build().toJSONObject()));
    jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
    return jwsObject.serialize();
  }

  private void setJwtAuthentication(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("jwt-token")
            .subject(userId.toString())
            .header("alg", "none")
            .claim("sub", userId.toString())
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @Nested
  @DisplayName("introspect()")
  class IntrospectTests {

    @Test
    void it_should_return_valid_true_when_token_is_well_signed_and_not_expired()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-introspect-1", Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS), null);
      when(invalidatedTokenRepository.existsById("jit-introspect-1")).thenReturn(false);

      // ===== ACT =====
      IntrospectResponse response = authenticationService.introspect(IntrospectRequest.builder().token(token).build());

      // ===== ASSERT =====
      assertTrue(response.isValid());

      // ===== VERIFY =====
      verify(invalidatedTokenRepository, times(1)).existsById("jit-introspect-1");
      verifyNoMoreInteractions(invalidatedTokenRepository);
    }

    @Test
    void it_should_return_valid_false_when_token_is_invalid() {
      // ===== ARRANGE =====
      String token = "broken.jwt.token";

      // ===== ACT =====
      IntrospectResponse response = authenticationService.introspect(IntrospectRequest.builder().token(token).build());

      // ===== ASSERT =====
      assertFalse(response.isValid());

      // ===== VERIFY =====
      verifyNoMoreInteractions(invalidatedTokenRepository);
    }

    @Test
    void it_should_return_valid_false_when_token_signature_is_invalid() throws JOSEException {
      // ===== ARRANGE =====
      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(USER_ID.toString())
              .issuer("school.edu")
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
              .jwtID("jit-invalid-signature")
              .build();
      var jwsObject =
          new com.nimbusds.jose.JWSObject(
              new JWSHeader(JWSAlgorithm.HS256), new Payload(claimsSet.toJSONObject()));
      jwsObject.sign(new MACSigner("different_signer_key_for_testing_32".getBytes()));

      // ===== ACT =====
      IntrospectResponse response =
          authenticationService.introspect(
              IntrospectRequest.builder().token(jwsObject.serialize()).build());

      // ===== ASSERT =====
      assertFalse(response.isValid());

      // ===== VERIFY =====
      verifyNoMoreInteractions(invalidatedTokenRepository);
    }
  }

  @Nested
  @DisplayName("login()")
  class LoginTests {

    @Test
    void it_should_return_token_when_credentials_are_valid_and_account_active() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder().email("linh.nguyen@fpt.edu.vn").password("ValidPass#123").build();
      Role studentRole = buildRole(PredefinedRole.STUDENT_ROLE, List.of("QUESTION_READ"));
      User user =
          buildUser(
              USER_ID,
              "linh.nguyen",
              "linh.nguyen@fpt.edu.vn",
              encodePassword("ValidPass#123"),
              Status.ACTIVE);
      user.setRoles(Set.of(studentRole));
      when(userRepository.findByEmailWithRolesAndPermissions("linh.nguyen@fpt.edu.vn"))
          .thenReturn(Optional.of(user));

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.login(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertNotNull(response.getToken()),
          () -> assertNotNull(response.getExpiryTime()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmailWithRolesAndPermissions("linh.nguyen@fpt.edu.vn");
      verify(userRepository, times(1)).save(user);
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_synchronize_seed_password_when_bcrypt_mismatch_but_seed_credential_matches() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder().email("admin@mathmaster.edu.vn").password("SeedPass#2026").build();
      Role adminRole = buildRole(PredefinedRole.ADMIN_ROLE, List.of("USER_MANAGE"));
      User user =
          buildUser(
              USER_ID,
              "math.admin",
              "admin@mathmaster.edu.vn",
              "$2a$10$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
              Status.ACTIVE);
      user.setRoles(Set.of(adminRole));

      InitProperties.UserConfig adminConfig = new InitProperties.UserConfig();
      adminConfig.setEmail("admin@mathmaster.edu.vn");
      adminConfig.setPassword("SeedPass#2026");

      when(userRepository.findByEmailWithRolesAndPermissions("admin@mathmaster.edu.vn"))
          .thenReturn(Optional.of(user));
      when(initProperties.isEnabled()).thenReturn(true);
      when(initProperties.getAdmin()).thenReturn(adminConfig);
      when(initProperties.getTeacher()).thenReturn(null);
      when(initProperties.getStudent()).thenReturn(null);

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.login(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response.getToken()),
          () -> assertNotNull(user.getLastLogin()),
          () -> assertTrue(user.getPassword().startsWith("$2a$")));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmailWithRolesAndPermissions("admin@mathmaster.edu.vn");
      verify(userRepository, times(2)).save(user);
      verify(initProperties, times(1)).isEnabled();
      verify(initProperties, times(1)).getAdmin();
      verifyNoMoreInteractions(userRepository, initProperties);
    }

    @Test
    void it_should_throw_unauthenticated_when_password_does_not_match_and_not_seed_account() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder().email("long.tran@fpt.edu.vn").password("WrongPass#2026").build();
      User user =
          buildUser(
              USER_ID,
              "long.tran",
              "long.tran@fpt.edu.vn",
              encodePassword("CorrectPass#2026"),
              Status.ACTIVE);
      when(userRepository.findByEmailWithRolesAndPermissions("long.tran@fpt.edu.vn"))
          .thenReturn(Optional.of(user));
      when(initProperties.isEnabled()).thenReturn(false);

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.login(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmailWithRolesAndPermissions("long.tran@fpt.edu.vn");
      verify(userRepository, never()).save(any(User.class));
      verify(initProperties, times(1)).isEnabled();
      verifyNoMoreInteractions(userRepository, initProperties);
    }

    @Test
    void it_should_throw_account_not_active_when_password_valid_but_status_inactive() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder().email("an.vo@fpt.edu.vn").password("ValidPass#123").build();
      User user =
          buildUser(
              USER_ID,
              "an.vo",
              "an.vo@fpt.edu.vn",
              encodePassword("ValidPass#123"),
              Status.INACTIVE);
      when(userRepository.findByEmailWithRolesAndPermissions("an.vo@fpt.edu.vn"))
          .thenReturn(Optional.of(user));

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.login(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmailWithRolesAndPermissions("an.vo@fpt.edu.vn");
      verify(userRepository, never()).save(any(User.class));
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_authenticate_when_teacher_seed_credential_matches() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder()
              .email("teacher.seed@mathmaster.edu.vn")
              .password("TeacherSeed#2026")
              .build();
      User user =
          buildUser(
              USER_ID,
              "teacher.seed",
              "teacher.seed@mathmaster.edu.vn",
              "plain-non-bcrypt",
              Status.ACTIVE);
      user.setRoles(Set.of(buildRole(PredefinedRole.TEACHER_ROLE, null)));
      when(userRepository.findByEmailWithRolesAndPermissions("teacher.seed@mathmaster.edu.vn"))
          .thenReturn(Optional.of(user));
      when(initProperties.isEnabled()).thenReturn(true);
      InitProperties.UserConfig admin = new InitProperties.UserConfig();
      admin.setEmail("admin@mathmaster.edu.vn");
      admin.setPassword("Admin#2026");
      InitProperties.UserConfig teacher = new InitProperties.UserConfig();
      teacher.setEmail("teacher.seed@mathmaster.edu.vn");
      teacher.setPassword("TeacherSeed#2026");
      when(initProperties.getAdmin()).thenReturn(admin);
      when(initProperties.getTeacher()).thenReturn(teacher);

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.login(request);

      // ===== ASSERT =====
      assertNotNull(response.getToken());

      // ===== VERIFY =====
      verify(userRepository, times(1))
          .findByEmailWithRolesAndPermissions("teacher.seed@mathmaster.edu.vn");
      verify(userRepository, times(2)).save(user);
      verify(initProperties, times(1)).isEnabled();
      verify(initProperties, times(1)).getAdmin();
      verify(initProperties, times(1)).getTeacher();
      verifyNoMoreInteractions(userRepository, initProperties);
    }

    @Test
    void it_should_authenticate_when_student_seed_credential_matches() {
      // ===== ARRANGE =====
      AuthenticationRequest request =
          AuthenticationRequest.builder()
              .email("student.seed@mathmaster.edu.vn")
              .password("StudentSeed#2026")
              .build();
      User user =
          buildUser(
              USER_ID,
              "student.seed",
              "student.seed@mathmaster.edu.vn",
              "plain-non-bcrypt",
              Status.ACTIVE);
      user.setRoles(Set.of(buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"))));
      when(userRepository.findByEmailWithRolesAndPermissions("student.seed@mathmaster.edu.vn"))
          .thenReturn(Optional.of(user));
      when(initProperties.isEnabled()).thenReturn(true);
      InitProperties.UserConfig admin = new InitProperties.UserConfig();
      admin.setEmail("admin@mathmaster.edu.vn");
      admin.setPassword("Admin#2026");
      InitProperties.UserConfig teacher = new InitProperties.UserConfig();
      teacher.setEmail("teacher.seed@mathmaster.edu.vn");
      teacher.setPassword("TeacherSeed#2026");
      InitProperties.UserConfig student = new InitProperties.UserConfig();
      student.setEmail("student.seed@mathmaster.edu.vn");
      student.setPassword("StudentSeed#2026");
      when(initProperties.getAdmin()).thenReturn(admin);
      when(initProperties.getTeacher()).thenReturn(teacher);
      when(initProperties.getStudent()).thenReturn(student);

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.login(request);

      // ===== ASSERT =====
      assertNotNull(response.getToken());

      // ===== VERIFY =====
      verify(userRepository, times(1))
          .findByEmailWithRolesAndPermissions("student.seed@mathmaster.edu.vn");
      verify(userRepository, times(2)).save(user);
      verify(initProperties, times(1)).isEnabled();
      verify(initProperties, times(1)).getAdmin();
      verify(initProperties, times(1)).getTeacher();
      verify(initProperties, times(1)).getStudent();
      verifyNoMoreInteractions(userRepository, initProperties);
    }
  }

  @Nested
  @DisplayName("logout()")
  class LogoutTests {

    @Test
    void it_should_save_invalidated_token_when_logout_token_is_valid() throws JOSEException, ParseException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-logout-1", Instant.now(), Instant.now().plus(30, ChronoUnit.MINUTES), null);
      when(invalidatedTokenRepository.existsById("jit-logout-1")).thenReturn(false);

      // ===== ACT =====
      authenticationService.logout(LogoutRequest.builder().token(token).build());

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(invalidatedTokenRepository, times(1)).existsById("jit-logout-1");
      verify(invalidatedTokenRepository, times(1)).save(any());
      verifyNoMoreInteractions(invalidatedTokenRepository);
    }

    @Test
    void it_should_ignore_logout_when_token_is_already_invalidated() throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-logout-2", Instant.now(), Instant.now().plus(30, ChronoUnit.MINUTES), null);
      when(invalidatedTokenRepository.existsById("jit-logout-2")).thenReturn(true);

      // ===== ACT =====
      assertDoesNotThrow(() -> authenticationService.logout(LogoutRequest.builder().token(token).build()));

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(invalidatedTokenRepository, times(1)).existsById("jit-logout-2");
      verify(invalidatedTokenRepository, never()).save(any());
      verifyNoMoreInteractions(invalidatedTokenRepository);
    }
  }

  @Nested
  @DisplayName("refreshToken()")
  class RefreshTokenTests {

    @Test
    void it_should_issue_new_token_and_blacklist_old_token_when_refresh_token_is_valid()
        throws JOSEException, ParseException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-refresh-1",
              Instant.now().minus(5, ChronoUnit.MINUTES),
              Instant.now().plus(5, ChronoUnit.MINUTES),
              null);
      when(invalidatedTokenRepository.existsById("jit-refresh-1")).thenReturn(false);
      User user =
          buildUser(
              USER_ID,
              "hai.pham",
              "hai.pham@fpt.edu.vn",
              encodePassword("RefreshPass#2026"),
              Status.ACTIVE);
      user.setRoles(Set.of(buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"))));
      when(userRepository.findByIdWithRolesAndPermissions(USER_ID)).thenReturn(Optional.of(user));

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.refreshToken(RefreshRequest.builder().token(token).build());

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertNotNull(response.getToken()),
          () -> assertNotNull(response.getExpiryTime()));

      // ===== VERIFY =====
      verify(invalidatedTokenRepository, times(1)).existsById("jit-refresh-1");
      verify(invalidatedTokenRepository, times(1)).save(any());
      verify(userRepository, times(1)).findByIdWithRolesAndPermissions(USER_ID);
      verifyNoMoreInteractions(invalidatedTokenRepository, userRepository);
    }

    @Test
    void it_should_throw_unauthenticated_when_user_not_found_during_refresh()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-refresh-2",
              Instant.now().minus(5, ChronoUnit.MINUTES),
              Instant.now().plus(5, ChronoUnit.MINUTES),
              null);
      when(invalidatedTokenRepository.existsById("jit-refresh-2")).thenReturn(false);
      when(userRepository.findByIdWithRolesAndPermissions(USER_ID)).thenReturn(Optional.empty());
      RefreshRequest refreshRequest = RefreshRequest.builder().token(token).build();

      // ===== ACT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> authenticationService.refreshToken(refreshRequest));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(invalidatedTokenRepository, times(1)).existsById("jit-refresh-2");
      verify(invalidatedTokenRepository, times(1)).save(any());
      verify(userRepository, times(1)).findByIdWithRolesAndPermissions(USER_ID);
      verifyNoMoreInteractions(invalidatedTokenRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("register()")
  class RegisterTests {

    @Test
    void it_should_register_inactive_student_and_send_confirmation_email_when_input_is_valid() {
      // ===== ARRANGE =====
      UserRegistrationRequest request =
          UserRegistrationRequest.builder()
              .userName("khoi.pham")
              .email("khoi.pham@fpt.edu.vn")
              .password("StrongPass#2026")
              .build();
      when(userRepository.existsByUserName("khoi.pham")).thenReturn(false);
      when(userRepository.existsByEmail("khoi.pham@fpt.edu.vn")).thenReturn(false);
      Role studentRole = buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"));
      when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE)).thenReturn(Optional.of(studentRole));

      User savedUser = buildUser(USER_ID, "khoi.pham", "khoi.pham@fpt.edu.vn", "encoded", Status.INACTIVE);
      when(userRepository.save(any(User.class))).thenReturn(savedUser);

      // ===== ACT =====
      UserRegisterResponse response = authenticationService.register(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(USER_ID, response.getId()),
          () -> assertEquals("khoi.pham@fpt.edu.vn", response.getEmail()),
          () -> assertEquals(Status.INACTIVE, response.getStatus()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName("khoi.pham");
      verify(userRepository, times(1)).existsByEmail("khoi.pham@fpt.edu.vn");
      verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
      verify(userRepository, times(1)).save(any(User.class));
      verify(emailService, times(1))
          .sendEmailConfirmation(
              org.mockito.ArgumentMatchers.eq("khoi.pham@fpt.edu.vn"),
              org.mockito.ArgumentMatchers.eq("khoi.pham"),
              org.mockito.ArgumentMatchers.contains("/confirm-email?token="));
      verifyNoMoreInteractions(userRepository, roleRepository, emailService);
    }

    @Test
    void it_should_throw_user_existed_when_username_is_duplicated() {
      // ===== ARRANGE =====
      UserRegistrationRequest request =
          UserRegistrationRequest.builder()
              .userName("khoi.pham")
              .email("another@fpt.edu.vn")
              .password("StrongPass#2026")
              .build();
      when(userRepository.existsByUserName("khoi.pham")).thenReturn(true);

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.register(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName("khoi.pham");
      verify(userRepository, never()).existsByEmail(any());
      verifyNoMoreInteractions(userRepository, roleRepository, emailService);
    }

    @Test
    void it_should_throw_email_already_exists_when_email_is_duplicated() {
      // ===== ARRANGE =====
      UserRegistrationRequest request =
          UserRegistrationRequest.builder()
              .userName("new.student")
              .email("khoi.pham@fpt.edu.vn")
              .password("StrongPass#2026")
              .build();
      when(userRepository.existsByUserName("new.student")).thenReturn(false);
      when(userRepository.existsByEmail("khoi.pham@fpt.edu.vn")).thenReturn(true);

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.register(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).existsByUserName("new.student");
      verify(userRepository, times(1)).existsByEmail("khoi.pham@fpt.edu.vn");
      verifyNoMoreInteractions(userRepository, roleRepository, emailService);
    }
  }

  @Nested
  @DisplayName("confirmEmail()")
  class ConfirmEmailTests {

    @Test
    void it_should_activate_account_when_confirmation_token_is_valid_and_user_inactive()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-confirm-1",
              Instant.now(),
              Instant.now().plus(1, ChronoUnit.HOURS),
              "email-confirmation");
      User user = buildUser(USER_ID, "duy.nguyen", "duy.nguyen@fpt.edu.vn", "encoded", Status.INACTIVE);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

      // ===== ACT =====
      authenticationService.confirmEmail(token);

      // ===== ASSERT =====
      assertEquals(Status.ACTIVE, user.getStatus());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(userRepository, times(1)).save(user);
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_throw_unauthenticated_when_confirmation_token_purpose_is_invalid()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-confirm-2",
              Instant.now(),
              Instant.now().plus(1, ChronoUnit.HOURS),
              "password-reset");

      // ===== ACT =====
      AppException exception = assertThrows(AppException.class, () -> authenticationService.confirmEmail(token));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_not_save_user_when_confirmation_token_valid_but_user_already_active()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-confirm-3",
              Instant.now(),
              Instant.now().plus(1, ChronoUnit.HOURS),
              "email-confirmation");
      User user = buildUser(USER_ID, "duy.nguyen", "duy.nguyen@fpt.edu.vn", "encoded", Status.ACTIVE);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

      // ===== ACT =====
      authenticationService.confirmEmail(token);

      // ===== ASSERT =====
      assertEquals(Status.ACTIVE, user.getStatus());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(userRepository, never()).save(any(User.class));
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_throw_unauthenticated_when_confirmation_token_is_expired() throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-confirm-expired",
              Instant.now().minus(2, ChronoUnit.HOURS),
              Instant.now().minus(1, ChronoUnit.HOURS),
              "email-confirmation");

      // ===== ACT =====
      AppException exception = assertThrows(AppException.class, () -> authenticationService.confirmEmail(token));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository);
    }
  }

  @Nested
  @DisplayName("forgotPassword()")
  class ForgotPasswordTests {

    @Test
    void it_should_send_reset_email_when_user_email_exists() {
      // ===== ARRANGE =====
      ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("trang.le@fpt.edu.vn").build();
      User user = buildUser(USER_ID, "trang.le", "trang.le@fpt.edu.vn", "encoded", Status.ACTIVE);
      when(userRepository.findByEmail("trang.le@fpt.edu.vn")).thenReturn(Optional.of(user));

      // ===== ACT =====
      authenticationService.forgotPassword(request);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmail("trang.le@fpt.edu.vn");
      verify(emailService, times(1))
          .sendPasswordResetEmail(
              org.mockito.ArgumentMatchers.eq("trang.le@fpt.edu.vn"),
              org.mockito.ArgumentMatchers.eq("trang.le"),
              org.mockito.ArgumentMatchers.contains("/reset-password?token="));
      verifyNoMoreInteractions(userRepository, emailService);
    }

    @Test
    void it_should_not_send_reset_email_when_email_not_found() {
      // ===== ARRANGE =====
      ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("ghost.user@fpt.edu.vn").build();
      when(userRepository.findByEmail("ghost.user@fpt.edu.vn")).thenReturn(Optional.empty());

      // ===== ACT =====
      authenticationService.forgotPassword(request);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByEmail("ghost.user@fpt.edu.vn");
      verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
      verifyNoMoreInteractions(userRepository, emailService);
    }
  }

  @Nested
  @DisplayName("resetPassword()")
  class ResetPasswordTests {

    @Test
    void it_should_reset_password_when_reset_token_is_valid_and_purpose_matches() throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-reset-1", Instant.now(), Instant.now().plus(10, ChronoUnit.MINUTES), "password-reset");
      User user = buildUser(USER_ID, "thao.ngo", "thao.ngo@fpt.edu.vn", "old", Status.ACTIVE);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
      ResetPasswordRequest request =
          ResetPasswordRequest.builder().token(token).newPassword("BrandNewPass#2026").build();

      // ===== ACT =====
      authenticationService.resetPassword(request);

      // ===== ASSERT =====
      assertTrue(user.getPassword().startsWith("$2a$"));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(userRepository, times(1)).save(user);
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_throw_unauthenticated_when_reset_token_purpose_is_not_password_reset()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-reset-2", Instant.now(), Instant.now().plus(10, ChronoUnit.MINUTES), "email-confirmation");
      ResetPasswordRequest request =
          ResetPasswordRequest.builder().token(token).newPassword("BrandNewPass#2026").build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.resetPassword(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_throw_user_not_existed_when_reset_token_is_valid_but_subject_user_missing()
        throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID, "jit-reset-3", Instant.now(), Instant.now().plus(10, ChronoUnit.MINUTES), "password-reset");
      when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
      ResetPasswordRequest request =
          ResetPasswordRequest.builder().token(token).newPassword("BrandNewPass#2026").build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.resetPassword(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verifyNoMoreInteractions(userRepository);
    }

    @Test
    void it_should_throw_unauthenticated_when_reset_token_is_expired() throws JOSEException {
      // ===== ARRANGE =====
      String token =
          createSignedToken(
              USER_ID,
              "jit-reset-expired",
              Instant.now().minus(30, ChronoUnit.MINUTES),
              Instant.now().minus(20, ChronoUnit.MINUTES),
              "password-reset");
      ResetPasswordRequest request =
          ResetPasswordRequest.builder().token(token).newPassword("BrandNewPass#2026").build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.resetPassword(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository);
    }
  }

  @Nested
  @DisplayName("selectRole()")
  class SelectRoleTests {

    @Test
    void it_should_throw_unauthenticated_when_security_context_is_not_jwt_authentication() {
      // ===== ARRANGE =====
      SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));
      RoleSelectionRequest request = RoleSelectionRequest.builder().role(PredefinedRole.STUDENT_ROLE).build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.selectRole(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository, roleRepository);
    }

    @Test
    void it_should_throw_user_existed_when_new_username_is_already_taken_by_other_user() {
      // ===== ARRANGE =====
      setJwtAuthentication(USER_ID);
      User user = buildUser(USER_ID, "existing.user", "existing.user@fpt.edu.vn", "encoded", Status.INACTIVE);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
      when(userRepository.existsByUserName("taken.username")).thenReturn(true);
      RoleSelectionRequest request =
          RoleSelectionRequest.builder()
              .role(PredefinedRole.STUDENT_ROLE)
              .userName("taken.username")
              .fullName("Existing User")
              .build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.selectRole(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(userRepository, times(1)).existsByUserName("taken.username");
      verify(roleRepository, never()).findByName(any());
      verifyNoMoreInteractions(userRepository, roleRepository);
    }

    @Test
    void it_should_update_profile_assign_student_role_and_return_token_when_request_is_valid() {
      // ===== ARRANGE =====
      setJwtAuthentication(OTHER_USER_ID);
      User user =
          buildUser(
              OTHER_USER_ID,
              "first.username",
              "first.username@fpt.edu.vn",
              "encoded",
              Status.INACTIVE);
      user.setRoles(Set.of(buildRole(PredefinedRole.TEACHER_ROLE, List.of("QUIZ_CREATE"))));
      when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user));
      when(userRepository.existsByUserName("new.student.username")).thenReturn(false);
      Role studentRole = buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ", "LESSON_VIEW"));
      when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE)).thenReturn(Optional.of(studentRole));
      when(userRepository.save(user)).thenReturn(user);
      RoleSelectionRequest request =
          RoleSelectionRequest.builder()
              .role(PredefinedRole.STUDENT_ROLE)
              .userName("new.student.username")
              .fullName("Nguyen Thi Hoc Sinh")
              .build();

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.selectRole(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertNotNull(response.getToken()),
          () -> assertEquals("new.student.username", user.getUserName()),
          () -> assertEquals("Nguyen Thi Hoc Sinh", user.getFullName()),
          () -> assertEquals(Status.ACTIVE, user.getStatus()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(OTHER_USER_ID);
      verify(userRepository, times(1)).existsByUserName("new.student.username");
      verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
      verify(userRepository, times(1)).save(user);
      verifyNoMoreInteractions(userRepository, roleRepository);
    }

    @Test
    void it_should_throw_user_not_existed_when_subject_not_found_in_select_role() {
      // ===== ARRANGE =====
      setJwtAuthentication(USER_ID);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
      RoleSelectionRequest request = RoleSelectionRequest.builder().role(PredefinedRole.STUDENT_ROLE).build();

      // ===== ACT =====
      AppException exception =
          assertThrows(AppException.class, () -> authenticationService.selectRole(request));

      // ===== ASSERT =====
      assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verifyNoMoreInteractions(userRepository, roleRepository);
    }

    @Test
    void it_should_keep_current_username_when_new_username_equals_current_value() {
      // ===== ARRANGE =====
      setJwtAuthentication(USER_ID);
      User user = buildUser(USER_ID, "same.username", "same.username@fpt.edu.vn", "encoded", Status.INACTIVE);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
      when(userRepository.existsByUserName("same.username")).thenReturn(true);
      Role studentRole = buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"));
      when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE)).thenReturn(Optional.of(studentRole));
      when(userRepository.save(user)).thenReturn(user);
      RoleSelectionRequest request =
          RoleSelectionRequest.builder()
              .role(PredefinedRole.STUDENT_ROLE)
              .userName("same.username")
              .fullName(" ")
              .build();

      // ===== ACT =====
      AuthenticationResponse response = authenticationService.selectRole(request);

      // ===== ASSERT =====
      assertNotNull(response.getToken());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(userRepository, times(1)).existsByUserName("same.username");
      verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
      verify(userRepository, times(1)).save(user);
      verifyNoMoreInteractions(userRepository, roleRepository);
    }
  }

  @Nested
  @DisplayName("googleLogin()")
  class GoogleLoginTests {

    @Test
    void it_should_throw_unauthenticated_when_google_verifier_returns_null()
        throws Exception {
      // ===== ARRANGE =====
      GoogleIdTokenVerifier verifier = Mockito.mock(GoogleIdTokenVerifier.class);
      GoogleIdToken parsedToken = Mockito.mock(GoogleIdToken.class);
      GoogleIdToken.Payload parsedPayload = Mockito.mock(GoogleIdToken.Payload.class);

      when(parsedToken.getPayload()).thenReturn(parsedPayload);
      when(parsedPayload.getAudience()).thenReturn("client-id");
      when(parsedPayload.getIssuer()).thenReturn("accounts.google.com");
      when(parsedPayload.getExpirationTimeSeconds()).thenReturn(9999999999L);
      when(parsedPayload.getIssuedAtTimeSeconds()).thenReturn(9999990000L);
      when(verifier.verify("google-bad-token")).thenReturn(null);
      GoogleAuthRequest googleAuthRequest = GoogleAuthRequest.builder().token("google-bad-token").build();

      try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
              Mockito.mockConstruction(
                  GoogleIdTokenVerifier.Builder.class,
                  (builder, context) -> {
                    when(builder.setAudience(any())).thenReturn(builder);
                    when(builder.build()).thenReturn(verifier);
                  });
          MockedStatic<GoogleIdToken> googleIdTokenMockedStatic = Mockito.mockStatic(GoogleIdToken.class)) {
        googleIdTokenMockedStatic
            .when(() -> GoogleIdToken.parse(any(), Mockito.eq("google-bad-token")))
            .thenReturn(parsedToken);

        // ===== ACT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> authenticationService.googleLogin(googleAuthRequest));

        // ===== ASSERT =====
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

        // ===== VERIFY =====
        assertEquals(1, mockedBuilder.constructed().size());
        verify(verifier, times(1)).verify("google-bad-token");
        verifyNoMoreInteractions(userRepository, roleRepository);
      }
    }

    @Test
    void it_should_return_token_when_google_user_already_exists() throws Exception {
      // ===== ARRANGE =====
      GoogleIdTokenVerifier verifier = Mockito.mock(GoogleIdTokenVerifier.class);
      GoogleIdToken parsedToken = Mockito.mock(GoogleIdToken.class);
      GoogleIdToken.Payload parsedPayload = Mockito.mock(GoogleIdToken.Payload.class);
      GoogleIdToken idToken = Mockito.mock(GoogleIdToken.class);
      GoogleIdToken.Payload payload = Mockito.mock(GoogleIdToken.Payload.class);

      when(parsedToken.getPayload()).thenReturn(parsedPayload);
      when(parsedPayload.getAudience()).thenReturn("client-id");
      when(parsedPayload.getIssuer()).thenReturn("accounts.google.com");
      when(parsedPayload.getExpirationTimeSeconds()).thenReturn(9999999999L);
      when(parsedPayload.getIssuedAtTimeSeconds()).thenReturn(9999990000L);

      when(verifier.verify("google-ok-token")).thenReturn(idToken);
      when(idToken.getPayload()).thenReturn(payload);
      when(payload.getEmail()).thenReturn("google.student@fpt.edu.vn");
      when(payload.get("name")).thenReturn("Google Student");
      when(payload.get("picture")).thenReturn("https://avatar.example/student.png");

      User existingUser =
          buildUser(USER_ID, "google.student", "google.student@fpt.edu.vn", "encoded", Status.ACTIVE);
      existingUser.setRoles(Set.of(buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"))));
      when(userRepository.findByEmailWithRolesAndPermissions("google.student@fpt.edu.vn"))
          .thenReturn(Optional.of(existingUser));

      try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
              Mockito.mockConstruction(
                  GoogleIdTokenVerifier.Builder.class,
                  (builder, context) -> {
                    when(builder.setAudience(any())).thenReturn(builder);
                    when(builder.build()).thenReturn(verifier);
                  });
          MockedStatic<GoogleIdToken> googleIdTokenMockedStatic = Mockito.mockStatic(GoogleIdToken.class)) {
        googleIdTokenMockedStatic
            .when(() -> GoogleIdToken.parse(any(), Mockito.eq("google-ok-token")))
            .thenReturn(parsedToken);

        // ===== ACT =====
        AuthenticationResponse response =
            authenticationService.googleLogin(GoogleAuthRequest.builder().token("google-ok-token").build());

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(response.getToken()),
            () -> assertFalse(response.isNewRegistration()));

        // ===== VERIFY =====
        assertEquals(1, mockedBuilder.constructed().size());
        verify(verifier, times(1)).verify("google-ok-token");
        verify(userRepository, times(1)).findByEmailWithRolesAndPermissions("google.student@fpt.edu.vn");
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository, roleRepository);
      }
    }

    @Test
    void it_should_auto_register_google_user_when_email_not_found() throws Exception {
      // ===== ARRANGE =====
      GoogleIdTokenVerifier verifier = Mockito.mock(GoogleIdTokenVerifier.class);
      GoogleIdToken parsedToken = Mockito.mock(GoogleIdToken.class);
      GoogleIdToken.Payload parsedPayload = Mockito.mock(GoogleIdToken.Payload.class);
      GoogleIdToken idToken = Mockito.mock(GoogleIdToken.class);
      GoogleIdToken.Payload payload = Mockito.mock(GoogleIdToken.Payload.class);

      when(parsedToken.getPayload()).thenReturn(parsedPayload);
      when(parsedPayload.getAudience()).thenReturn("client-id");
      when(parsedPayload.getIssuer()).thenReturn("accounts.google.com");
      when(parsedPayload.getExpirationTimeSeconds()).thenReturn(9999999999L);
      when(parsedPayload.getIssuedAtTimeSeconds()).thenReturn(9999990000L);

      when(verifier.verify("google-new-token")).thenReturn(idToken);
      when(idToken.getPayload()).thenReturn(payload);
      when(payload.getEmail()).thenReturn("new.google.student@fpt.edu.vn");
      when(payload.get("name")).thenReturn("New Google Student");
      when(payload.get("picture")).thenReturn("https://avatar.example/new-student.png");

      Role studentRole = buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"));
      when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE)).thenReturn(Optional.of(studentRole));
      User persisted = buildUser(USER_ID, "new.google.student", "new.google.student@fpt.edu.vn", "encoded", Status.ACTIVE);
      persisted.setRoles(Set.of(studentRole));
      when(userRepository.findByEmailWithRolesAndPermissions("new.google.student@fpt.edu.vn"))
          .thenReturn(Optional.empty())
          .thenReturn(Optional.of(persisted));
      when(userRepository.save(any(User.class))).thenReturn(persisted);

      try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
              Mockito.mockConstruction(
                  GoogleIdTokenVerifier.Builder.class,
                  (builder, context) -> {
                    when(builder.setAudience(any())).thenReturn(builder);
                    when(builder.build()).thenReturn(verifier);
                  });
          MockedStatic<GoogleIdToken> googleIdTokenMockedStatic = Mockito.mockStatic(GoogleIdToken.class)) {
        googleIdTokenMockedStatic
            .when(() -> GoogleIdToken.parse(any(), Mockito.eq("google-new-token")))
            .thenReturn(parsedToken);

        // ===== ACT =====
        AuthenticationResponse response =
            authenticationService.googleLogin(GoogleAuthRequest.builder().token("google-new-token").build());

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(response.getToken()),
            () -> assertTrue(response.isNewRegistration()));

        // ===== VERIFY =====
        assertEquals(1, mockedBuilder.constructed().size());
        verify(verifier, times(1)).verify("google-new-token");
        verify(userRepository, times(2)).findByEmailWithRolesAndPermissions("new.google.student@fpt.edu.vn");
        verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
        verify(userRepository, times(1)).save(any(User.class));
        verifyNoMoreInteractions(userRepository, roleRepository);
      }
    }
  }

  @Nested
  @DisplayName("private mappings")
  class PrivateMappingsTests {

    @Test
    void it_should_map_user_response_with_null_roles_when_user_has_no_roles() throws Exception {
      // ===== ARRANGE =====
      User user = buildUser(USER_ID, "private.user", "private.user@fpt.edu.vn", "encoded", Status.ACTIVE);
      user.setRoles(null);
      Method mapMethod = AuthenticationServiceImpl.class.getDeclaredMethod("mapToUserResponse", User.class);
      mapMethod.setAccessible(true);

      // ===== ACT =====
      UserResponse response = (UserResponse) mapMethod.invoke(authenticationService, user);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(USER_ID, response.getId()),
          () -> assertEquals("private.user@fpt.edu.vn", response.getEmail()),
          () -> assertEquals(null, response.getRoles()));

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository, roleRepository, invalidatedTokenRepository, emailService);
    }

    @Test
    void it_should_map_user_response_with_role_names_when_user_has_roles() throws Exception {
      // ===== ARRANGE =====
      User user = buildUser(USER_ID, "private.user", "private.user@fpt.edu.vn", "encoded", Status.ACTIVE);
      user.setRoles(Set.of(buildRole(PredefinedRole.STUDENT_ROLE, List.of("COURSE_READ"))));
      Method mapMethod = AuthenticationServiceImpl.class.getDeclaredMethod("mapToUserResponse", User.class);
      mapMethod.setAccessible(true);

      // ===== ACT =====
      UserResponse response = (UserResponse) mapMethod.invoke(authenticationService, user);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response.getRoles()),
          () -> assertTrue(response.getRoles().contains(PredefinedRole.STUDENT_ROLE)));

      // ===== VERIFY =====
      verifyNoMoreInteractions(userRepository, roleRepository, invalidatedTokenRepository, emailService);
    }
  }
}
