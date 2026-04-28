package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.RejectWithdrawalRequest;
import com.fptu.math_master.dto.request.VerifyWithdrawalOtpRequest;
import com.fptu.math_master.dto.request.WithdrawalRequestDto;
import com.fptu.math_master.dto.response.AdminWithdrawalRequestResponse;
import com.fptu.math_master.dto.response.WithdrawalRequestResponse;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.entity.WithdrawalRequest;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.enums.WithdrawalStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.repository.WithdrawalRequestRepository;
import com.fptu.math_master.service.EmailService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.service.WithdrawalService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {

  WithdrawalRequestRepository withdrawalRequestRepository;
  WalletRepository walletRepository;
  UserRepository userRepository;
  TransactionRepository transactionRepository;
  EmailService emailService;
  UploadService uploadService;
  PasswordEncoder passwordEncoder;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // ─── User: Submit Withdrawal Request ────────────────────────────────────

  @Override
  @Transactional
  public WithdrawalRequestResponse createWithdrawalRequest(UUID userId, WithdrawalRequestDto request) {
    log.info("User {} submitting withdrawal request for amount {}", userId, request.getAmount());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // 1. Verify current password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new AppException(ErrorCode.INCORRECT_PASSWORD);
    }

    // 2. Check wallet balance >= amount
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
      throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // 3. Ensure no concurrent pending request
    boolean hasPending = withdrawalRequestRepository.existsByUserIdAndStatusIn(
        userId, List.of(WithdrawalStatus.PENDING_VERIFY, WithdrawalStatus.PENDING_ADMIN));
    if (hasPending) {
      throw new AppException(ErrorCode.WITHDRAWAL_REQUEST_ALREADY_EXISTS);
    }

    // 4. Generate 6-digit OTP and hash it
    String rawOtp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    String hashedOtp = passwordEncoder.encode(rawOtp);

    // 5. Persist the request
    WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
        .wallet(wallet)
        .user(user)
        .amount(request.getAmount())
        .bankName(request.getBankName())
        .bankAccountNumber(request.getBankAccountNumber())
        .bankAccountName(request.getBankAccountName())
        .status(WithdrawalStatus.PENDING_VERIFY)
        .otpCode(hashedOtp)
        .otpExpiry(Instant.now().plus(10, ChronoUnit.MINUTES))
        .build();

    withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);
    log.info("WithdrawalRequest {} created for user {}", withdrawalRequest.getId(), userId);

    // 6. Send OTP email (async — non-blocking)
    emailService.sendWithdrawalOtpEmail(user.getEmail(), user.getUserName(), rawOtp, request.getAmount());

    return toUserResponse(withdrawalRequest);
  }

  // ─── User: Verify OTP ────────────────────────────────────────────────────

  @Override
  @Transactional
  public WithdrawalRequestResponse verifyOtp(UUID userId, VerifyWithdrawalOtpRequest request) {
    log.info("User {} verifying OTP for withdrawal request {}", userId, request.getWithdrawalRequestId());

    WithdrawalRequest wr = withdrawalRequestRepository
        .findByIdAndUserId(request.getWithdrawalRequestId(), userId)
        .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

    // Status must be PENDING_VERIFY
    if (wr.getStatus() != WithdrawalStatus.PENDING_VERIFY) {
      throw new AppException(ErrorCode.INVALID_WITHDRAWAL_STATUS);
    }

    // OTP expiry check — cancel and fail if expired
    if (Instant.now().isAfter(wr.getOtpExpiry())) {
      wr.setStatus(WithdrawalStatus.CANCELLED);
      wr.setOtpCode(null);
      wr.setOtpExpiry(null);
      withdrawalRequestRepository.save(wr);
      log.warn("OTP expired for withdrawal request {}", wr.getId());
      throw new AppException(ErrorCode.OTP_EXPIRED);
    }

    // OTP code verification
    if (!passwordEncoder.matches(request.getOtpCode(), wr.getOtpCode())) {
      throw new AppException(ErrorCode.INVALID_OTP);
    }

    // Deduct balance immediately when OTP verified — lock funds while request is pending
    Wallet wallet = walletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    if (wallet.getBalance().compareTo(wr.getAmount()) < 0) {
      throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
    }
    int updated = walletRepository.decrementBalance(wallet.getId(), wr.getAmount());
    if (updated == 0) {
      throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
    }
    log.info("Balance {} deducted from wallet {} upon OTP verify for withdrawal {}",
        wr.getAmount(), wallet.getId(), wr.getId());

    // Transition to PENDING_ADMIN — clear OTP fields
    wr.setStatus(WithdrawalStatus.PENDING_ADMIN);
    wr.setOtpCode(null);
    wr.setOtpExpiry(null);
    wr = withdrawalRequestRepository.save(wr);
    log.info("WithdrawalRequest {} verified — now PENDING_ADMIN", wr.getId());

    return toUserResponse(wr);
  }

  // ─── User: Get Own Requests ───────────────────────────────────────────────

  @Override
  public Page<WithdrawalRequestResponse> getMyRequests(UUID userId, WithdrawalStatus status, Pageable pageable) {
    if (status != null) {
      return withdrawalRequestRepository.findByUserIdAndStatus(userId, status, pageable)
          .map(this::toUserResponse);
    }
    return withdrawalRequestRepository.findByUserId(userId, pageable)
        .map(this::toUserResponse);
  }

  // ─── User: Cancel Request ─────────────────────────────────────────────────

  @Override
  @Transactional
  public WithdrawalRequestResponse cancelRequest(UUID userId, UUID withdrawalRequestId) {
    log.info("User {} cancelling withdrawal request {}", userId, withdrawalRequestId);

    WithdrawalRequest wr = withdrawalRequestRepository
        .findByIdAndUserId(withdrawalRequestId, userId)
        .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

    if (wr.getStatus() != WithdrawalStatus.PENDING_VERIFY
        && wr.getStatus() != WithdrawalStatus.PENDING_ADMIN) {
      throw new AppException(ErrorCode.INVALID_WITHDRAWAL_STATUS);
    }

    // Refund balance if already deducted (i.e. OTP was verified → PENDING_ADMIN)
    if (wr.getStatus() == WithdrawalStatus.PENDING_ADMIN) {
      walletRepository.incrementBalance(wr.getWallet().getId(), wr.getAmount());
      log.info("Refunded {} to wallet {} on cancellation of withdrawal {}",
          wr.getAmount(), wr.getWallet().getId(), wr.getId());
    }

    wr.setStatus(WithdrawalStatus.CANCELLED);
    wr.setOtpCode(null);
    wr.setOtpExpiry(null);
    wr = withdrawalRequestRepository.save(wr);
    log.info("WithdrawalRequest {} cancelled by user {}", wr.getId(), userId);

    return toUserResponse(wr);
  }

  // ─── Admin: List Requests ─────────────────────────────────────────────────

  @Override
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public Page<AdminWithdrawalRequestResponse> getAdminRequests(
      WithdrawalStatus status, String search, Pageable pageable) {
    String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
    String statusStr = (status != null) ? status.name() : null;
    // Native query already has ORDER BY wr.created_at DESC; strip the Pageable sort to prevent
    // Spring Data JPA from appending camelCase column names (e.g. "wr.createdAt") that PostgreSQL rejects.
    Pageable unsorted = org.springframework.data.domain.PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize());
    return withdrawalRequestRepository.findAllForAdmin(statusStr, normalizedSearch, unsorted)
        .map(this::toAdminResponse);
  }

  // ─── Admin: Process (Approve) Withdrawal ─────────────────────────────────

  @Override
  @Transactional
  public AdminWithdrawalRequestResponse processWithdrawal(
      UUID withdrawalRequestId, MultipartFile proofImage, String adminNote) {
    log.info("Admin processing withdrawal request {}", withdrawalRequestId);

    WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalRequestId)
        .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

    if (wr.getStatus() != WithdrawalStatus.PENDING_ADMIN) {
      throw new AppException(ErrorCode.INVALID_WITHDRAWAL_STATUS);
    }

    // Upload proof image (reuse UploadService — invariant #6)
    String proofImageUrl = uploadService.uploadFile(proofImage, "withdrawal-proofs");

    // Balance was already deducted at OTP verify time — just get wallet for transaction record
    Wallet wallet = walletRepository.findByUserId(wr.getUser().getId())
        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    // Create the withdrawal Transaction (invariant #5: exactly one per request)
    Transaction transaction = Transaction.builder()
        .wallet(wallet)
        .amount(wr.getAmount())
        .type(TransactionType.WITHDRAWAL)
        .status(TransactionStatus.SUCCESS)
        .description("Rút tiền thủ công — " + wr.getBankName() + " " + wr.getBankAccountNumber())
        .transactionDate(Instant.now())
        .build();
    transaction = transactionRepository.save(transaction);

    // Update WithdrawalRequest to SUCCESS
    wr.setStatus(WithdrawalStatus.SUCCESS);
    wr.setProofImageUrl(proofImageUrl);
    wr.setAdminNote(adminNote);
    wr.setTransaction(transaction);
    wr.setProcessedAt(Instant.now());
    wr = withdrawalRequestRepository.save(wr);
    log.info("WithdrawalRequest {} processed successfully — transaction {}", wr.getId(), transaction.getId());

    // Send bill email (async)
    User user = wr.getUser();
    emailService.sendWithdrawalSuccessEmail(
        user.getEmail(),
        user.getUserName(),
        wr.getAmount(),
        wr.getBankName(),
        wr.getBankAccountNumber(),
        wr.getBankAccountName(),
        proofImageUrl,
        transaction.getId().toString(),
        wr.getProcessedAt());

    return toAdminResponse(wr);
  }

  // ─── Admin: Reject Withdrawal ─────────────────────────────────────────────

  @Override
  @Transactional
  public AdminWithdrawalRequestResponse rejectWithdrawal(
      UUID withdrawalRequestId, RejectWithdrawalRequest request) {
    log.info("Admin rejecting withdrawal request {}", withdrawalRequestId);

    WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalRequestId)
        .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_REQUEST_NOT_FOUND));

    if (wr.getStatus() != WithdrawalStatus.PENDING_ADMIN
        && wr.getStatus() != WithdrawalStatus.PROCESSING) {
      throw new AppException(ErrorCode.INVALID_WITHDRAWAL_STATUS);
    }

    // Refund balance — was already deducted at OTP verify time
    walletRepository.incrementBalance(wr.getWallet().getId(), wr.getAmount());
    log.info("Refunded {} to wallet {} on rejection of withdrawal {}",
        wr.getAmount(), wr.getWallet().getId(), wr.getId());

    wr.setStatus(WithdrawalStatus.REJECTED);
    wr.setAdminNote(request.getReason());
    wr.setProcessedAt(Instant.now());
    wr = withdrawalRequestRepository.save(wr);
    log.info("WithdrawalRequest {} rejected", wr.getId());

    // Send rejection email (async)
    User user = wr.getUser();
    emailService.sendWithdrawalRejectedEmail(
        user.getEmail(), user.getUserName(), wr.getAmount(), request.getReason());

    return toAdminResponse(wr);
  }

  // ─── Mappers ─────────────────────────────────────────────────────────────

  private WithdrawalRequestResponse toUserResponse(WithdrawalRequest wr) {
    return WithdrawalRequestResponse.builder()
        .withdrawalRequestId(wr.getId())
        .amount(wr.getAmount())
        .bankName(wr.getBankName())
        .bankAccountNumber(wr.getBankAccountNumber())
        .bankAccountName(wr.getBankAccountName())
        .status(wr.getStatus())
        .proofImageUrl(wr.getProofImageUrl())
        .adminNote(wr.getAdminNote())
        .transactionId(wr.getTransaction() != null ? wr.getTransaction().getId() : null)
        .processedAt(wr.getProcessedAt())
        .createdAt(wr.getCreatedAt())
        .updatedAt(wr.getUpdatedAt())
        .build();
  }

  private AdminWithdrawalRequestResponse toAdminResponse(WithdrawalRequest wr) {
    User user = wr.getUser();
    return AdminWithdrawalRequestResponse.builder()
        .withdrawalRequestId(wr.getId())
        .userId(user.getId())
        .userName(user.getUserName())
        .userEmail(user.getEmail())
        .amount(wr.getAmount())
        .bankName(wr.getBankName())
        .bankAccountNumber(wr.getBankAccountNumber())
        .bankAccountName(wr.getBankAccountName())
        .status(wr.getStatus())
        .proofImageUrl(wr.getProofImageUrl())
        .adminNote(wr.getAdminNote())
        .transactionId(wr.getTransaction() != null ? wr.getTransaction().getId() : null)
        .processedAt(wr.getProcessedAt())
        .createdAt(wr.getCreatedAt())
        .updatedAt(wr.getUpdatedAt())
        .build();
  }
}
