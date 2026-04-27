package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.RejectWithdrawalRequest;
import com.fptu.math_master.dto.request.VerifyWithdrawalOtpRequest;
import com.fptu.math_master.dto.request.WithdrawalRequestDto;
import com.fptu.math_master.dto.response.AdminWithdrawalRequestResponse;
import com.fptu.math_master.dto.response.WithdrawalRequestResponse;
import com.fptu.math_master.enums.WithdrawalStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface WithdrawalService {

  /** User: submit a withdrawal request — sends OTP email */
  WithdrawalRequestResponse createWithdrawalRequest(UUID userId, WithdrawalRequestDto request);

  /** User: verify the 6-digit OTP — transitions to PENDING_ADMIN */
  WithdrawalRequestResponse verifyOtp(UUID userId, VerifyWithdrawalOtpRequest request);

  /** User: get paginated own withdrawal history */
  Page<WithdrawalRequestResponse> getMyRequests(UUID userId, WithdrawalStatus status, Pageable pageable);

  /** User: cancel a request in PENDING_VERIFY or PENDING_ADMIN */
  WithdrawalRequestResponse cancelRequest(UUID userId, UUID withdrawalRequestId);

  /** Admin: paginated list with optional status + full-text search */
  Page<AdminWithdrawalRequestResponse> getAdminRequests(
      WithdrawalStatus status, String search, Pageable pageable);

  /** Admin: process (approve) — uploads proof image, debits wallet, creates Transaction */
  AdminWithdrawalRequestResponse processWithdrawal(
      UUID withdrawalRequestId, MultipartFile proofImage, String adminNote);

  /** Admin: reject — sets REJECTED and sends rejection email */
  AdminWithdrawalRequestResponse rejectWithdrawal(
      UUID withdrawalRequestId, RejectWithdrawalRequest request);
}
