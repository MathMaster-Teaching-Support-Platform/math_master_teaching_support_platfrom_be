package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TransactionResponse;
import com.fptu.math_master.dto.response.WalletResponse;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Wallet", description = "APIs for managing user wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    WalletService walletService;

    @Operation(summary = "Create wallet", description = "Create a new wallet for current user")
    @PostMapping("/create")
    public ApiResponse<WalletResponse> createWallet() {
        UUID userId = getCurrentUserId();
        return ApiResponse.<WalletResponse>builder()
            .result(walletService.createWallet(userId))
            .build();
    }

    @Operation(summary = "Get my wallet", description = "Get current user's wallet information")
    @GetMapping("/my-wallet")
    public ApiResponse<WalletResponse> getMyWallet() {
        UUID userId = getCurrentUserId();
        return ApiResponse.<WalletResponse>builder()
            .result(walletService.getOrCreateWallet(userId))
            .build();
    }

    @Operation(summary = "Get my transactions", description = "Get transaction history")
    @GetMapping("/transactions")
    public ApiResponse<Page<TransactionResponse>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.<Page<TransactionResponse>>builder()
            .result(walletService.getMyTransactions(userId, pageable))
            .build();
    }

    @Operation(summary = "Get transactions by status",
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

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}
