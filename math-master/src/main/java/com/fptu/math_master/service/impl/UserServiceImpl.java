package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.ChangePasswordRequest;
import com.fptu.math_master.dto.request.UserCreationRequest;
import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.dto.request.UserUpdateRequest;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;
  RoleRepository roleRepository;
  PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserResponse createUser(UserCreationRequest request) {
    log.info("Creating user with username: {}", request.getUserName());

    // Check if username already exists
    if (userRepository.existsByUserName(request.getUserName())) {
      throw new AppException(ErrorCode.USER_EXISTED);
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    // Build user entity
    User user = User.builder()
      .userName(request.getUserName())
      .password(passwordEncoder.encode(request.getPassword()))
      .fullName(request.getFullName())
      .email(request.getEmail())
      .phoneNumber(request.getPhoneNumber())
      .gender(request.getGender())
      .avatar(request.getAvatar())
      .dob(request.getDob())
      .code(request.getCode())
      .status(Status.ACTIVE)
      .build();

    // Set roles if provided
    if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      Set<Role> roles = new HashSet<>();
      for (String roleName : request.getRoles()) {
        Role role = roleRepository.findByName(roleName)
          .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        roles.add(role);
      }
      user.setRoles(roles);
    }

    // Save user
    user = userRepository.save(user);

    log.info("User created successfully with id: {}", user.getId());
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
    log.info("Updating user with id: {}", userId);

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Update password if provided
    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    // Update basic fields
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }

    if (request.getEmail() != null) {
      // Check if email is being changed and if new email already exists
      if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
      }
      user.setEmail(request.getEmail());
    }

    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }

    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }

    if (request.getAvatar() != null) {
      user.setAvatar(request.getAvatar());
    }

    if (request.getDob() != null) {
      user.setDob(request.getDob());
    }

    if (request.getCode() != null) {
      user.setCode(request.getCode());
    }

    if (request.getStatus() != null) {
      user.setStatus(request.getStatus());
    }

    // Update roles if provided
    if (request.getRoles() != null) {
      Set<Role> roles = new HashSet<>();
      for (String roleName : request.getRoles()) {
        Role role = roleRepository.findByName(roleName)
          .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        roles.add(role);
      }
      user.setRoles(roles);
    }

    user = userRepository.save(user);

    log.info("User updated successfully with id: {}", user.getId());
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public void deleteUser(UUID userId) {
    log.info("Soft deleting user with id: {}", userId);

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Soft delete by setting status to DELETED
    user.setStatus(Status.DELETED);
    userRepository.save(user);

    log.info("User soft deleted successfully with id: {}", userId);
  }

  @Override
  public UserResponse getUserById(UUID userId) {
    log.info("Getting user with id: {}", userId);

    User user = userRepository.findByIdWithRoles(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return mapToUserResponse(user);
  }

  @Override
  public List<UserResponse> getAllUsers() {
    log.info("Getting all users");

    List<User> users = userRepository.findAll();

    return users.stream()
      .map(this::mapToUserResponse)
      .collect(Collectors.toList());
  }

  @Override
  public Page<UserResponse> getAllUsers(Pageable pageable) {
    log.info("Getting all users with pagination");

    Page<User> users = userRepository.findAll(pageable);

    return users.map(this::mapToUserResponse);
  }

  @Override
  public UserResponse getMyInfo() {
    log.info("Getting current user info");

    var context = SecurityContextHolder.getContext();
    String email = context.getAuthentication().getName();

    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateMyInfo(UserUpdateRequest request) {
    log.info("Updating current user info");

    var context = SecurityContextHolder.getContext();
    String email = context.getAuthentication().getName();

    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Update password if provided
    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    // Update basic fields
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }

    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }

    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }

    if (request.getAvatar() != null) {
      user.setAvatar(request.getAvatar());
    }

    if (request.getDob() != null) {
      user.setDob(request.getDob());
    }

    user = userRepository.save(user);

    log.info("Current user info updated successfully");
    return mapToUserResponse(user);
  }

  @Override
  public Page<UserResponse> searchUsers(UserSearchRequest request, Pageable pageable) {
    log.info("Searching users with criteria: {}", request);
    Page<User> users = userRepository.findAll(
        com.fptu.math_master.repository.UserSpecification.searchUsers(request), 
        pageable
    );
    return users.map(this::mapToUserResponse);
  }

  @Override
  @Transactional
  public UserResponse banUser(UUID userId, String reason) {
    log.info("Banning user with id: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    if (user.getStatus() == Status.BANNED) {
      throw new AppException(ErrorCode.USER_ALREADY_BANNED);
    }
    user.setStatus(Status.BANNED);
    user.setBanReason(reason);
    user.setBanDate(Instant.now());
    user = userRepository.save(user);
    log.info("User banned successfully with id: {}", userId);
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse unbanUser(UUID userId) {
    log.info("Unbanning user with id: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    if (user.getStatus() != Status.BANNED) {
      throw new AppException(ErrorCode.USER_NOT_BANNED);
    }
    user.setStatus(Status.ACTIVE);
    user.setBanReason(null);
    user.setBanDate(null);
    user = userRepository.save(user);
    log.info("User unbanned successfully with id: {}", userId);
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse disableUser(UUID userId) {
    log.info("Disabling user with id: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    if (user.getStatus() == Status.INACTIVE) {
      throw new AppException(ErrorCode.USER_ALREADY_DISABLED);
    }
    user.setStatus(Status.INACTIVE);
    user = userRepository.save(user);
    log.info("User disabled successfully with id: {}", userId);
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public UserResponse enableUser(UUID userId) {
    log.info("Enabling user with id: {}", userId);
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    if (user.getStatus() == Status.ACTIVE) {
      throw new AppException(ErrorCode.USER_ALREADY_ENABLED);
    }
    if (user.getStatus() == Status.DELETED) {
      throw new AppException(ErrorCode.USER_NOT_EXISTED);
    }
    user.setStatus(Status.ACTIVE);
    user = userRepository.save(user);
    log.info("User enabled successfully with id: {}", userId);
    return mapToUserResponse(user);
  }

  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    log.info("Changing password for current user");
    var context = SecurityContextHolder.getContext();
    String email = context.getAuthentication().getName();
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      throw new AppException(ErrorCode.INCORRECT_PASSWORD);
    }
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new AppException(ErrorCode.PASSWORD_MISMATCH);
    }
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    log.info("Password changed successfully for user: {}", email);
  }

  private UserResponse mapToUserResponse(User user) {
    Set<String> roles = null;
    if (user.getRoles() != null) {
      roles = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toSet());
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
      .createdDate(user.getCreatedDate())
      .createdBy(user.getCreatedBy())
      .updatedDate(user.getUpdatedDate())
      .updatedBy(user.getUpdatedBy())
      .build();
  }
}

