package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TransactionResponse;
import com.fptu.math_master.dto.response.WalletResponse;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user wallet operations. Provides endpoints for wallet creation,
 * retrieval, and transaction history. All endpoints require authentication via Bearer token.
 *
 * @author Math Master Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Wallet", description = "APIs for managing user wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

  WalletService walletService;

  /**
   * Creates a new wallet for the currently authenticated user.
   *
   * @return ApiResponse containing the newly created wallet information
   * @throws IllegalStateException if user already has a wallet
   */
  @Operation(summary = "Create wallet", description = "Create a new wallet for current user")
  @PostMapping("/create")
  public ApiResponse<WalletResponse> createWallet() {
    UUID userId = getCurrentUserId();
    return ApiResponse.<WalletResponse>builder().result(walletService.createWallet(userId)).build();
  }

  /**
   * Retrieves the wallet information for the currently authenticated user. If the user doesn't have
   * a wallet, one will be automatically created.
   *
   * @return ApiResponse containing the user's wallet information
   */
  @Operation(summary = "Get my wallet", description = "Get current user's wallet information")
  @GetMapping("/my-wallet")
  public ApiResponse<WalletResponse> getMyWallet() {
    UUID userId = getCurrentUserId();
    return ApiResponse.<WalletResponse>builder()
        .result(walletService.getOrCreateWallet(userId))
        .build();
  }

  /**
   * Retrieves paginated transaction history for the currently authenticated user. Transactions are
   * sorted by creation date in descending order (newest first).
   *
   * @param page the page number (0-indexed), default is 0
   * @param size the number of transactions per page, default is 10
   * @return ApiResponse containing a page of transaction records
   */
  @Operation(summary = "Get my transactions", description = "Get transaction history")
  @GetMapping("/transactions")
  public ApiResponse<Page<TransactionResponse>> getMyTransactions(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    UUID userId = getCurrentUserId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ApiResponse.<Page<TransactionResponse>>builder()
        .result(walletService.getMyTransactions(userId, pageable))
        .build();
  }

  /**
   * Retrieves paginated transaction history filtered by transaction status. Transactions are sorted
   * by creation date in descending order (newest first).
   *
   * @param status the transaction status to filter by (e.g., SUCCESS, PENDING, FAILED)
   * @param page the page number (0-indexed), default is 0
   * @param size the number of transactions per page, default is 10
   * @return ApiResponse containing a page of filtered transaction records
   */
  @Operation(
      summary = "Get transactions by status",
      description = "Get transaction history filtered by status")
  @GetMapping("/transactions/status/{status}")
  public ApiResponse<Page<TransactionResponse>> getMyTransactionsByStatus(
      @PathVariable TransactionStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    UUID userId = getCurrentUserId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ApiResponse.<Page<TransactionResponse>>builder()
        .result(walletService.getMyTransactionsByStatus(userId, status, pageable))
        .build();
  }

  /**
   * Extracts the user ID from the current security context.
   *
   * @return UUID of the currently authenticated user
   * @throws IllegalArgumentException if the user ID cannot be parsed
   */
  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }
}
