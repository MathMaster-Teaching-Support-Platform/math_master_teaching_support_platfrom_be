package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.DepositRequest;
import com.fptu.math_master.dto.request.PayOSWebhookRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.PaymentLinkResponse;
import com.fptu.math_master.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Payment", description = "APIs for payment processing")
public class PaymentController {

    PaymentService paymentService;

    @Operation(summary = "Create deposit payment",
               description = "Create a payment link for depositing money to wallet")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/deposit")
    public ApiResponse<PaymentLinkResponse> createDeposit(
            @Valid @RequestBody DepositRequest request) {
        UUID userId = getCurrentUserId();
        return ApiResponse.<PaymentLinkResponse>builder()
            .result(paymentService.createDepositPayment(request, userId))
            .build();
    }

    @Operation(summary = "PayOS Webhook",
               description = "Webhook endpoint for receiving payment notifications from PayOS")
    @PostMapping("/webhook")
    public ApiResponse<Void> handleWebhook(@RequestBody PayOSWebhookRequest webhookRequest) {
        log.info("Received webhook from PayOS: {}", webhookRequest);
        try {
            paymentService.handleWebhook(webhookRequest);
            return ApiResponse.<Void>builder()
                .message("Webhook processed successfully")
                .build();
        } catch (Exception e) {
            log.error("Error processing webhook, but returning 200 OK to PayOS: {}", e.getMessage(), e);
            // Always return 200 OK to PayOS to prevent retry storms
            return ApiResponse.<Void>builder()
                .message("Webhook received, processing failed but acknowledged")
                .build();
        }
    }

    @Operation(summary = "Manual confirm transaction (Admin/Dev only)",
               description = "Manually confirm a pending transaction as successful. Use this for transactions where webhook was not received.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/confirm/{orderCode}")
    public ApiResponse<String> confirmTransaction(@PathVariable Long orderCode) {
        log.info("Manual confirmation requested for orderCode: {}", orderCode);
        paymentService.manualConfirmTransaction(orderCode);
        return ApiResponse.<String>builder()
            .message("Transaction confirmed successfully")
            .result("Wallet balance updated")
            .build();
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}
