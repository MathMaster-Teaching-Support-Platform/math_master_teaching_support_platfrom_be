package com.fptu.math_master.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.request.ChangePasswordRequest;
import com.fptu.math_master.dto.request.UserCreationRequest;
import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.dto.request.UserUpdateRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  UserService userService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Create a new user",
      description =
          "Create a new user account with the provided information. Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
    log.info("REST request to create user: {}", request.getUserName());
    return ApiResponse.<UserResponse>builder().result(userService.createUser(request)).build();
  }

  @PutMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Update user by ID",
      description =
          "Update an existing user's information by user ID. Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> updateUser(
      @PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest request) {
    log.info("REST request to update user: {}", userId);
    return ApiResponse.<UserResponse>builder()
        .result(userService.updateUser(userId, request))
        .build();
  }

  @DeleteMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Delete user by ID",
      description = "Delete a user account by user ID. Only accessible by ADMIN role.")
  public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
    log.info("REST request to delete user: {}", userId);
    userService.deleteUser(userId);
    return ApiResponse.<Void>builder().message("User deleted successfully").build();
  }

  @GetMapping("/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  @Operation(summary = "Get user by ID", description = "Retrieve user information by user ID.")
  public ApiResponse<UserResponse> getUserById(@PathVariable UUID userId) {
    log.info("REST request to get user: {}", userId);
    return ApiResponse.<UserResponse>builder().result(userService.getUserById(userId)).build();
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get all users",
      description = "Retrieve a list of all users. Only accessible by ADMIN role.")
  public ApiResponse<List<UserResponse>> getAllUsers() {
    log.info("REST request to get all users");
    return ApiResponse.<List<UserResponse>>builder().result(userService.getAllUsers()).build();
  }

  @GetMapping("/page")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get all users with pagination",
      description = "Retrieve a paginated list of all users. Only accessible by ADMIN role.")
  public ApiResponse<Page<UserResponse>> getAllUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDirection) {
    log.info("REST request to get all users with pagination");

    Sort.Direction direction =
        sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ApiResponse.<Page<UserResponse>>builder()
        .result(userService.getAllUsers(pageable))
        .build();
  }

  @GetMapping("/my-info")
  @Operation(
      summary = "Get current user information",
      description = "Retrieve the authenticated user's own information.")
  public ApiResponse<UserResponse> getMyInfo() {
    log.info("REST request to get current user info");
    return ApiResponse.<UserResponse>builder().result(userService.getMyInfo()).build();
  }

  @PutMapping("/my-info")
  @Operation(
      summary = "Update current user information",
      description = "Update the authenticated user's own information.")
  public ApiResponse<UserResponse> updateMyInfo(@Valid @RequestBody UserUpdateRequest request) {
    log.info("REST request to update current user info");
    return ApiResponse.<UserResponse>builder().result(userService.updateMyInfo(request)).build();
  }

  @PostMapping("/search")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Search users with filters",
      description =
          "Search and filter users based on various criteria with pagination. Only accessible by ADMIN role.")
  public ApiResponse<Page<UserResponse>> searchUsers(
      @RequestBody UserSearchRequest request,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDirection) {
    log.info("REST request to search users with criteria: {}", request);

    Sort.Direction direction =
        sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ApiResponse.<Page<UserResponse>>builder()
        .result(userService.searchUsers(request, pageable))
        .build();
  }

  @PutMapping("/{userId}/ban")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Ban a user",
      description = "Ban a user by setting their status to BANNED. Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> banUser(
      @PathVariable UUID userId,
      @RequestParam(required = false, defaultValue = "Violated terms of service") String reason) {
    log.info("REST request to ban user: {} with reason: {}", userId, reason);
    return ApiResponse.<UserResponse>builder().result(userService.banUser(userId, reason)).build();
  }

  @PutMapping("/{userId}/unban")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Unban a user",
      description =
          "Unban a user by restoring their status to ACTIVE. Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> unbanUser(@PathVariable UUID userId) {
    log.info("REST request to unban user: {}", userId);
    return ApiResponse.<UserResponse>builder().result(userService.unbanUser(userId)).build();
  }

  @PutMapping("/{userId}/disable")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Disable a user",
      description =
          "Disable a user by setting their status to INACTIVE (soft delete). Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> disableUser(@PathVariable UUID userId) {
    log.info("REST request to disable user: {}", userId);
    return ApiResponse.<UserResponse>builder().result(userService.disableUser(userId)).build();
  }

  @PutMapping("/{userId}/enable")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Enable a user",
      description =
          "Enable a user by setting their status to ACTIVE. Only accessible by ADMIN role.")
  public ApiResponse<UserResponse> enableUser(@PathVariable UUID userId) {
    log.info("REST request to enable user: {}", userId);
    return ApiResponse.<UserResponse>builder().result(userService.enableUser(userId)).build();
  }

  @PutMapping("/change-password")
  @Operation(
      summary = "Change password",
      description =
          "Change the current user's password by providing current password and new password.")
  public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    log.info("REST request to change password for current user");
    userService.changePassword(request);
    return ApiResponse.<Void>builder().message("Password changed successfully").build();
  }

  @GetMapping("/admin/recent")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get recent users",
      description = "Returns the most recently registered users, sorted by registration date descending. Only accessible by ADMIN role.")
  public ApiResponse<Page<UserResponse>> getRecentUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("REST request to get recent users, page={}, size={}", page, size);
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<UserResponse>>builder()
        .result(userService.getRecentUsers(pageable))
        .build();
  }
}
