package com.fptu.math_master.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.fptu.math_master.dto.response.AdminUserListResponse;

import jakarta.servlet.http.HttpServletResponse;

public interface AdminUserService {

  AdminUserListResponse listUsers(String role, String search, String status, Pageable pageable);

  String resetPassword(UUID userId);

  void sendEmail(UUID userId, String subject, String body);

  void exportUsersToExcel(String role, String search, String status, HttpServletResponse response);
}
