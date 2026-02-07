package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.ChangePasswordRequest;
import com.fptu.math_master.dto.request.UserCreationRequest;
import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.dto.request.UserUpdateRequest;
import com.fptu.math_master.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
  UserResponse createUser(UserCreationRequest request);

  UserResponse updateUser(Integer userId, UserUpdateRequest request);

  void deleteUser(Integer userId);

  UserResponse getUserById(Integer userId);

  List<UserResponse> getAllUsers();

  Page<UserResponse> getAllUsers(Pageable pageable);

  UserResponse getMyInfo();

  UserResponse updateMyInfo(UserUpdateRequest request);

  // Search and filtering
  Page<UserResponse> searchUsers(UserSearchRequest request, Pageable pageable);

  // Ban/Unban operations
  UserResponse banUser(Integer userId, String reason);

  UserResponse unbanUser(Integer userId);

  // Soft delete operations
  UserResponse disableUser(Integer userId);

  UserResponse enableUser(Integer userId);

  // Password management
  void changePassword(ChangePasswordRequest request);
}

