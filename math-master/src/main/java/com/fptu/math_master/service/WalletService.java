package com.fptu.math_master.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.response.TransactionResponse;
import com.fptu.math_master.dto.response.WalletResponse;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.WalletRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WalletService {

  WalletRepository walletRepository;
  UserRepository userRepository;
  TransactionRepository transactionRepository;

  @Transactional
  public WalletResponse createWallet(UUID userId) {
    log.info("Creating wallet for user: {}", userId);

    // Check if user exists
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Check if wallet already exists
    if (walletRepository.existsByUserId(userId)) {
      throw new AppException(ErrorCode.WALLET_ALREADY_EXISTS);
    }

    // Create wallet
    Wallet wallet =
        Wallet.builder().user(user).balance(BigDecimal.ZERO).status(Status.ACTIVE).build();

    wallet = walletRepository.save(wallet);
    log.info("Wallet created successfully for user: {}", userId);

    return mapToWalletResponse(wallet);
  }

  public WalletResponse getMyWallet(UUID userId) {
    log.info("Getting wallet for user: {}", userId);

    Wallet wallet =
        walletRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    return mapToWalletResponse(wallet);
  }

  public WalletResponse getOrCreateWallet(UUID userId) {
    log.info("Getting or creating wallet for user: {}", userId);

    return walletRepository
        .findByUserId(userId)
        .map(this::mapToWalletResponse)
        .orElseGet(() -> createWallet(userId));
  }

  @Transactional
  public void addBalance(UUID walletId, BigDecimal amount) {
    log.info("Adding balance {} to wallet: {}", amount, walletId);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AppException(ErrorCode.INVALID_AMOUNT);
    }

    Wallet wallet =
        walletRepository
            .findById(walletId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    wallet.setBalance(wallet.getBalance().add(amount));
    walletRepository.save(wallet);

    log.info("Balance added successfully. New balance: {}", wallet.getBalance());
  }

  @Transactional
  public void deductBalance(UUID walletId, BigDecimal amount) {
    log.info("Deducting balance {} from wallet: {}", amount, walletId);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AppException(ErrorCode.INVALID_AMOUNT);
    }

    Wallet wallet =
        walletRepository
            .findById(walletId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    wallet.setBalance(wallet.getBalance().subtract(amount));
    walletRepository.save(wallet);

    log.info("Balance deducted successfully. New balance: {}", wallet.getBalance());
  }

  public Page<TransactionResponse> getMyTransactions(UUID userId, Pageable pageable) {
    log.info("Getting transactions for user: {}", userId);

    Wallet wallet =
        walletRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    return transactionRepository
        .findByWalletId(wallet.getId(), pageable)
        .map(this::mapToTransactionResponse);
  }

  public Page<TransactionResponse> getMyTransactionsByStatus(
      UUID userId, TransactionStatus status, Pageable pageable) {
    log.info("Getting transactions with status {} for user: {}", status, userId);

    Wallet wallet =
        walletRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    return transactionRepository
        .findByWalletIdAndStatus(wallet.getId(), status, pageable)
        .map(this::mapToTransactionResponse);
  }

  private WalletResponse mapToWalletResponse(Wallet wallet) {
    BigDecimal totalDeposited = transactionRepository.sumByWalletIdAndTypeAndStatus(
        wallet.getId(), TransactionType.DEPOSIT, TransactionStatus.SUCCESS);
    BigDecimal totalSpent = transactionRepository.sumByWalletIdAndTypeAndStatus(
        wallet.getId(), TransactionType.PAYMENT, TransactionStatus.SUCCESS);
    long transactionCount = transactionRepository.countByWalletId(wallet.getId());

    return WalletResponse.builder()
        .walletId(wallet.getId())
        .userId(wallet.getUser().getId())
        .balance(wallet.getBalance())
        .totalDeposited(totalDeposited)
        .totalSpent(totalSpent)
        .transactionCount(transactionCount)
        .status(wallet.getStatus())
        .createdAt(wallet.getCreatedAt())
        .updatedAt(wallet.getUpdatedAt())
        .build();
  }

  private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    return TransactionResponse.builder()
        .transactionId(transaction.getId())
        .walletId(transaction.getWallet().getId())
        .orderCode(transaction.getOrderCode())
        .amount(transaction.getAmount())
        .type(transaction.getType())
        .status(transaction.getStatus())
        .description(transaction.getDescription())
        .paymentLinkId(transaction.getPaymentLinkId())
        .referenceCode(transaction.getReferenceCode())
        .transactionDate(transaction.getTransactionDate())
        .expiresAt(transaction.getExpiresAt())
        .createdAt(transaction.getCreatedAt())
        .updatedAt(transaction.getUpdatedAt())
        .build();
  }
}
