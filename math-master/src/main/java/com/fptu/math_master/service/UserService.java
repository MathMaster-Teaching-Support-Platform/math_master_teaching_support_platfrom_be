package com.fptu.math_master.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.fptu.math_master.dto.request.ChangePasswordRequest;
import com.fptu.math_master.dto.request.UserCreationRequest;
import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.dto.request.UserUpdateRequest;
import com.fptu.math_master.dto.response.UserResponse;

public interface UserService {
  UserResponse createUser(UserCreationRequest request);

  UserResponse updateUser(UUID userId, UserUpdateRequest request);

  void deleteUser(UUID userId);

  UserResponse getUserById(UUID userId);

  List<UserResponse> getAllUsers();

  Page<UserResponse> getAllUsers(Pageable pageable);

  UserResponse getMyInfo();

  UserResponse updateMyInfo(UserUpdateRequest request);

  // Search and filtering
  Page<UserResponse> searchUsers(UserSearchRequest request, Pageable pageable);

  // Ban/Unban operations
  UserResponse banUser(UUID userId, String reason);

  UserResponse unbanUser(UUID userId);

  // Soft delete operations
  UserResponse disableUser(UUID userId);

  UserResponse enableUser(UUID userId);

  // Password management
  void changePassword(ChangePasswordRequest request);

  Page<UserResponse> getRecentUsers(Pageable pageable);

  // Avatar upload
  String uploadAvatar(MultipartFile file);
}
