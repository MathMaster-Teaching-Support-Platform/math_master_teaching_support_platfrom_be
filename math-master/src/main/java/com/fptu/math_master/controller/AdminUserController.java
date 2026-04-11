package com.fptu.math_master.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.request.AdminSendEmailRequest;
import com.fptu.math_master.dto.response.AdminUserListResponse;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.service.AdminUserService;
import com.fptu.math_master.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin User Management", description = "APIs for admin to manage users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AdminUserService adminUserService;
  private final UserService userService;

  @GetMapping
  @Operation(
      summary = "List users with stats",
      description = "Returns paginated user list with global stats (total, admins, teachers, students, active). "
          + "Supports filter by role (TEACHER|STUDENT_ONLY|ADMIN|all), status, search, sort, and date range.")
  public ApiResponse<AdminUserListResponse> listUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "all") String role,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "all") String status,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) Instant createdFrom,
      @RequestParam(required = false) Instant createdTo) {

    Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    AdminUserListResponse result = adminUserService.listUsers(role, search, status, sortBy, sortOrder, createdFrom, createdTo, pageable);
    return ApiResponse.<AdminUserListResponse>builder().result(result).build();
  }

  @GetMapping("/{userId}")
  @Operation(summary = "Get user detail", description = "Retrieve full detail of a user by ID.")
  public ApiResponse<UserResponse> getUserDetail(@PathVariable UUID userId) {
    log.info("Admin request to get user detail: {}", userId);
    return ApiResponse.<UserResponse>builder().result(userService.getUserById(userId)).build();
  }

  @PatchMapping("/{userId}/status")
  @Operation(
      summary = "Toggle user status",
      description = "Set user status to active or inactive. Accepts status=ACTIVE or status=INACTIVE.")
  public ApiResponse<UserResponse> updateStatus(
      @PathVariable UUID userId,
      @RequestParam String status) {

    log.info("Admin request to set user {} status to {}", userId, status);
    UserResponse result;
    if ("ACTIVE".equalsIgnoreCase(status)) {
      result = userService.enableUser(userId);
    } else if ("INACTIVE".equalsIgnoreCase(status)) {
      result = userService.disableUser(userId);
    } else {
      throw new IllegalArgumentException("status must be ACTIVE or INACTIVE");
    }
    return ApiResponse.<UserResponse>builder().result(result).build();
  }

  @PostMapping("/{userId}/reset-password")
  @Operation(
      summary = "Reset user password",
      description = "Admin resets a user's password. A temporary password is generated and emailed to the user.")
  public ApiResponse<Void> resetPassword(@PathVariable UUID userId) {
    log.info("Admin request to reset password for userId={}", userId);
    adminUserService.resetPassword(userId);
    return ApiResponse.<Void>builder()
        .message("Password reset successfully. Temporary password sent to user's email.")
        .build();
  }

  @PostMapping("/{userId}/send-email")
  @Operation(
      summary = "Send email to user",
      description = "Admin sends a custom email to a specific user.")
  public ApiResponse<Void> sendEmail(
      @PathVariable UUID userId,
      @Valid @RequestBody AdminSendEmailRequest request) {

    log.info("Admin request to send email to userId={}, subject={}", userId, request.getSubject());
    adminUserService.sendEmail(userId, request.getSubject(), request.getBody());
    return ApiResponse.<Void>builder().message("Email sent successfully.").build();
  }

  @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(
      summary = "Export users to Excel",
      description = "Downloads an Excel (.xlsx) file of all users matching the given filters.")
  public void exportUsers(
      @RequestParam(defaultValue = "all") String role,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "all") String status,
      HttpServletResponse response) {

    log.info("Admin request to export users: role={}, search={}, status={}", role, search, status);
    adminUserService.exportUsersToExcel(role, search, status, response);
  }
}
