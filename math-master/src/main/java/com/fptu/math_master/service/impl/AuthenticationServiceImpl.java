package com.fptu.math_master.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fptu.math_master.configuration.properties.InitProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.request.RoleSelectionRequest;
import com.fptu.math_master.dto.request.UserRegistrationRequest;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.fptu.math_master.dto.response.UserRegisterResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.entity.InvalidatedToken;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.InvalidatedTokenRepository;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AuthenticationService;
import com.fptu.math_master.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
  UserRepository userRepository;
  InvalidatedTokenRepository invalidatedTokenRepository;
  RoleRepository roleRepository;
  InitProperties initProperties;
  EmailService emailService;

  @NonFinal
  @Value("${jwt.signerKey}")
  protected String SIGNER_KEY;

  @NonFinal
  @Value("${jwt.valid-duration}")
  protected long VALID_DURATION;

  @NonFinal
  @Value("${jwt.refreshable-duration}")
  protected long REFRESHABLE_DURATION;

  @NonFinal
  @Value("${app.frontend-url:http://localhost:3000}")
  protected String FRONTEND_URL;

  @NonFinal
  @Value("${app.backend-url:http://localhost:8080}")
  protected String BACKEND_URL;

  @Override
  public IntrospectResponse introspect(IntrospectRequest request) {
    var token = request.getToken();
    boolean isValid = true;

    try {
      verifyToken(token, false);
    } catch (AppException | JOSEException | ParseException e) {
      isValid = false;
    }

    return IntrospectResponse.builder().valid(isValid).build();
  }

  @Override
  @Transactional
  public AuthenticationResponse login(AuthenticationRequest request) {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    var user =
        userRepository
            .findByEmailWithRolesAndPermissions(request.getEmail())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

    if (!authenticated && isSeedAccountCredentialMatch(request.getEmail(), request.getPassword())) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
      userRepository.save(user);
      authenticated = true;
      log.info("Synchronized seeded account password on login for email={}", request.getEmail());
    }

    if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

    if (user.getStatus() != Status.ACTIVE) {
      throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    var token = generateToken(user);

    return AuthenticationResponse.builder()
        .token(token)
        .expiryTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .build();
  }

  private boolean isSeedAccountCredentialMatch(String email, String rawPassword) {
    if (initProperties == null || !initProperties.isEnabled()) {
      return false;
    }

    InitProperties.UserConfig admin = initProperties.getAdmin();
    if (admin != null
        && email.equalsIgnoreCase(admin.getEmail())
        && rawPassword.equals(admin.getPassword())) {
      return true;
    }

    InitProperties.UserConfig teacher = initProperties.getTeacher();
    if (teacher != null
        && email.equalsIgnoreCase(teacher.getEmail())
        && rawPassword.equals(teacher.getPassword())) {
      return true;
    }

    InitProperties.UserConfig student = initProperties.getStudent();
    return student != null
        && email.equalsIgnoreCase(student.getEmail())
        && rawPassword.equals(student.getPassword());
  }

  @Override
  @Transactional
  public void logout(LogoutRequest request) throws ParseException, JOSEException {
    try {
      var signToken = verifyToken(request.getToken(), true);

      String jit = signToken.getJWTClaimsSet().getJWTID();
      Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

      InvalidatedToken invalidatedToken =
          InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

      invalidatedTokenRepository.save(invalidatedToken);
    } catch (AppException exception) {
      log.info("Token already expired");
    }
  }

  @Override
  @Transactional
  public AuthenticationResponse googleLogin(
      com.fptu.math_master.dto.request.GoogleAuthRequest request)
      throws GeneralSecurityException, IOException {

    String clientId = "299660266172-38kfomfcv0pcvrhrg0pas04rhfskqn8u.apps.googleusercontent.com";
    GoogleIdTokenVerifier verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .build();

    GoogleIdToken parsedToken = GoogleIdToken.parse(new GsonFactory(), request.getToken());
    log.info("Google Token Parsed. Audience: {}, Issuer: {}, ExpirationTimeSeconds: {}, IssuedAtTimeSeconds: {}, Now: {}",
      parsedToken.getPayload().getAudience(), parsedToken.getPayload().getIssuer(),
      parsedToken.getPayload().getExpirationTimeSeconds(), parsedToken.getPayload().getIssuedAtTimeSeconds(),
      System.currentTimeMillis() / 1000);

    GoogleIdToken idToken = verifier.verify(request.getToken());
    if (idToken != null) {
      GoogleIdToken.Payload payload = idToken.getPayload();

      String email = payload.getEmail();
      String name = (String) payload.get("name");
      String pictureUrl = (String) payload.get("picture");

      // Check if user exists
      var userOpt = userRepository.findByEmailWithRolesAndPermissions(email);
      User user;

      boolean isNewUser = false;
      if (userOpt.isPresent()) {
        user = userOpt.get();
      } else {
        // Auto register
        isNewUser = true;
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        String randomPassword = UUID.randomUUID().toString();

        user =
            User.builder()
                .userName(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5))
                .password(passwordEncoder.encode(randomPassword))
                .fullName(name)
                .email(email)
                .avatar(pictureUrl)
                .status(Status.ACTIVE)
                .build();

        Role userRole =
            roleRepository
                .findByName(PredefinedRole.STUDENT_ROLE)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        user = userRepository.save(user);
        log.info("Auto registered Google user with id: {}", user.getId());

        // Fetch again to ensure roles and permissions are loaded optimally
        user = userRepository.findByEmailWithRolesAndPermissions(email).orElse(user);
      }

      var token = generateToken(user);
      return AuthenticationResponse.builder()
          .token(token)
          .expiryTime(
              new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
          .newRegistration(isNewUser)
          .build();
    } else {
      log.error("Google verify() returned null for token request.");
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  @Override
  @Transactional
  public AuthenticationResponse refreshToken(RefreshRequest request)
      throws ParseException, JOSEException {
    var signedJWT = verifyToken(request.getToken(), true);

    var jit = signedJWT.getJWTClaimsSet().getJWTID();
    var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

    InvalidatedToken invalidatedToken =
        InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

    invalidatedTokenRepository.save(invalidatedToken);

    var userId = signedJWT.getJWTClaimsSet().getSubject();

    var user =
        userRepository
            .findByIdWithRolesAndPermissions(UUID.fromString(userId))
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    var token = generateToken(user);

    return AuthenticationResponse.builder()
        .token(token)
        .expiryTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .build();
  }

  @Override
  @Transactional
  public UserRegisterResponse register(UserRegistrationRequest request) {
    log.info("Registering new user with username: {}", request.getUserName());

    // Check if username already exists
    if (userRepository.existsByUserName(request.getUserName())) {
      throw new AppException(ErrorCode.USER_EXISTED);
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    // Build user entity — status = INACTIVE until email is confirmed
    User user =
        User.builder()
            .userName(request.getUserName())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .status(Status.INACTIVE)
            .build();

    // Default role is always STUDENT
    Role userRole =
        roleRepository
            .findByName(PredefinedRole.STUDENT_ROLE)
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    Set<Role> roles = new HashSet<>();
    roles.add(userRole);
    user.setRoles(roles);

    user = userRepository.save(user);

    // Send confirmation email
    String confirmationToken = generateConfirmationToken(user);
    String confirmationUrl = FRONTEND_URL + "/confirm-email?token=" + confirmationToken;
    emailService.sendEmailConfirmation(user.getEmail(), user.getUserName(), confirmationUrl);

    log.info("User registered with id: {}. Confirmation email sent.", user.getId());
    return mapToUserRegisterResponse(user);
  }

  @Override
  @Transactional
  public void confirmEmail(String token) {
    try {
      JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
      SignedJWT signedJWT = SignedJWT.parse(token);

      boolean verified = signedJWT.verify(verifier);
      Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

      if (!verified || !expiryTime.after(new Date())) {
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      }

      String purpose = signedJWT.getJWTClaimsSet().getStringClaim("purpose");
      if (!"email-confirmation".equals(purpose)) {
        throw new AppException(ErrorCode.UNAUTHENTICATED);
      }

      String userId = signedJWT.getJWTClaimsSet().getSubject();
      User user =
          userRepository
              .findById(UUID.fromString(userId))
              .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

      if (user.getStatus() == Status.INACTIVE) {
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        log.info("Email confirmed and account activated for user id: {}", userId);
      }
    } catch (JOSEException | ParseException e) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private String generateConfirmationToken(User user) {
    JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

    JWTClaimsSet jwtClaimsSet =
        new JWTClaimsSet.Builder()
            .subject(user.getId().toString())
            .issuer("school.edu")
            .issueTime(new Date())
            .expirationTime(new Date(Instant.now().plus(24, ChronoUnit.HOURS).toEpochMilli()))
            .jwtID(UUID.randomUUID().toString())
            .claim("purpose", "email-confirmation")
            .build();

    Payload payload = new Payload(jwtClaimsSet.toJSONObject());
    JWSObject jwsObject = new JWSObject(header, payload);

    try {
      jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
      return jwsObject.serialize();
    } catch (JOSEException e) {
      log.error("Cannot create confirmation token", e);
      throw new RuntimeException(e);
    }
  }

  private String generateToken(User user) {
    JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

    JWTClaimsSet jwtClaimsSet =
        new JWTClaimsSet.Builder()
            .subject(user.getId().toString())
            .issuer("school.edu")
            .issueTime(new Date())
            .expirationTime(
                new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
            .jwtID(UUID.randomUUID().toString())
            .claim("scope", buildScope(user))
            .claim("email", user.getEmail())
            .build();

    Payload payload = new Payload(jwtClaimsSet.toJSONObject());

    JWSObject jwsObject = new JWSObject(header, payload);

    try {
      jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
      return jwsObject.serialize();
    } catch (JOSEException e) {
      log.error("Cannot create token", e);
      throw new RuntimeException(e);
    }
  }

  private SignedJWT verifyToken(String token, boolean isRefresh)
      throws JOSEException, ParseException {
    JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

    SignedJWT signedJWT = SignedJWT.parse(token);

    Date expiryTime =
        (isRefresh)
            ? new Date(
                signedJWT
                    .getJWTClaimsSet()
                    .getIssueTime()
                    .toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                    .toEpochMilli())
            : signedJWT.getJWTClaimsSet().getExpirationTime();

    var verified = signedJWT.verify(verifier);

    if (!(verified && expiryTime.after(new Date())))
      throw new AppException(ErrorCode.UNAUTHENTICATED);

    if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
      throw new AppException(ErrorCode.UNAUTHENTICATED);

    return signedJWT;
  }

  private String buildScope(User user) {
    StringJoiner stringJoiner = new StringJoiner(" ");

    if (!CollectionUtils.isEmpty(user.getRoles())) {
      user.getRoles()
          .forEach(
              role -> {
                stringJoiner.add("ROLE_" + role.getName());

                // Add all permissions for this role
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                  role.getPermissions()
                      .forEach(permission -> stringJoiner.add(permission.getCode()));
                }
              });
    }

    return stringJoiner.toString();
  }

  @Override
  @Transactional
  public AuthenticationResponse selectRole(RoleSelectionRequest request) {
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    if (!(auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    String userId = jwtAuth.getToken().getSubject();
    var user =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Update info if provided
    if (request.getUserName() != null && !request.getUserName().isBlank()) {
      if (userRepository.existsByUserName(request.getUserName())
          && !request.getUserName().equals(user.getUserName())) {
        throw new AppException(ErrorCode.USER_EXISTED);
      }
      user.setUserName(request.getUserName());
    }

    if (request.getFullName() != null && !request.getFullName().isBlank()) {
      user.setFullName(request.getFullName());
    }

    // Assign role — always STUDENT
    Role userRole =
        roleRepository
            .findByName(PredefinedRole.STUDENT_ROLE)
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

    Set<Role> roles = new HashSet<>();
    roles.add(userRole);
    user.setRoles(roles);
    user.setStatus(Status.ACTIVE);

    user = userRepository.save(user);

    var token = generateToken(user);
    return AuthenticationResponse.builder()
        .token(token)
        .expiryTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .build();
  }

  private UserRegisterResponse mapToUserRegisterResponse(User user) {
    return UserRegisterResponse.builder()
        .id(user.getId())
        .userName(user.getUserName())
        .email(user.getEmail())
        .status(user.getStatus())
        .createdDate(user.getCreatedAt())
        .createdBy(user.getCreatedByName())
        .updatedDate(user.getUpdatedAt())
        .updatedBy(user.getUpdatedByName())
        .build();
  }

  private UserResponse mapToUserResponse(User user) {
    Set<String> roles = null;
    if (user.getRoles() != null) {
      roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    return UserResponse.builder()
        .id(user.getId())
        .userName(user.getUserName())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .phoneNumber(user.getPhoneNumber())
        .gender(user.getGender())
        .avatar(user.getAvatar())
        .dob(user.getDob())
        .code(user.getCode())
        .status(user.getStatus())
        .banReason(user.getBanReason())
        .banDate(user.getBanDate())
        .roles(roles)
        .createdDate(user.getCreatedAt())
        .createdBy(user.getCreatedByName())
        .updatedDate(user.getUpdatedAt())
        .updatedBy(user.getUpdatedByName())
        .build();
  }
}
