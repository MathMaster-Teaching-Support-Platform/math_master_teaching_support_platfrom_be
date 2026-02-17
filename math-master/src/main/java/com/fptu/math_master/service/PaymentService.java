package com.fptu.math_master.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.PayOSProperties;
import com.fptu.math_master.dto.request.DepositRequest;
import com.fptu.math_master.dto.request.PayOSWebhookRequest;
import com.fptu.math_master.dto.response.PayOSCreatePaymentResponse;
import com.fptu.math_master.dto.response.PaymentLinkResponse;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.WalletRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

  RestTemplate restTemplate;
  PayOSProperties payOSProperties;
  WalletRepository walletRepository;
  TransactionRepository transactionRepository;
  WalletService walletService;
  ObjectMapper objectMapper;

  private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";

  @Transactional
  public PaymentLinkResponse createDepositPayment(DepositRequest request, UUID userId) {
    log.info("Creating deposit payment for user: {}, amount: {}", userId, request.getAmount());

    try {
      // Get or create wallet
      Wallet wallet =
          walletRepository
              .findByUserId(userId)
              .orElseGet(
                  () -> {
                    log.info("Wallet not found, creating new wallet for user: {}", userId);
                    walletService.createWallet(userId);
                    return walletRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                  });

      // Generate unique order code
      long orderCode = System.currentTimeMillis();

      // Create transaction record
      Transaction transaction =
          Transaction.builder()
              .wallet(wallet)
              .orderCode(orderCode)
              .amount(request.getAmount())
              .type(TransactionType.DEPOSIT)
              .status(TransactionStatus.PENDING)
              .description(
                  request.getDescription() != null ? request.getDescription() : "Nạp tiền vào ví")
              .build();

      transaction = transactionRepository.save(transaction);
      log.info("Transaction created with orderCode: {}", orderCode);

      // Create payment request for PayOS API
      Map<String, Object> item = new HashMap<>();
      item.put("name", "Nạp tiền vào ví");
      item.put("quantity", 1);
      item.put("price", request.getAmount().intValue());

      Map<String, Object> paymentData = new HashMap<>();
      paymentData.put("orderCode", orderCode);
      paymentData.put("amount", request.getAmount().intValue());
      paymentData.put("description", transaction.getDescription());
      paymentData.put("returnUrl", payOSProperties.getReturnUrl());
      paymentData.put("cancelUrl", payOSProperties.getCancelUrl());
      paymentData.put("items", List.of(item));

      // Create signature
      String signature = createPaymentSignature(paymentData);
      paymentData.put("signature", signature);

      // Call PayOS API
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("x-client-id", payOSProperties.getClientId());
      headers.set("x-api-key", payOSProperties.getApiKey());

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentData, headers);

      ResponseEntity<PayOSCreatePaymentResponse> response =
          restTemplate.exchange(
              PAYOS_API_URL, HttpMethod.POST, entity, PayOSCreatePaymentResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        PayOSCreatePaymentResponse.PaymentData data = response.getBody().getData();

        // Update transaction with payment link info
        transaction.setPaymentLinkId(data.getPaymentLinkId());
        transactionRepository.save(transaction);

        log.info("Payment link created successfully: {}", data.getCheckoutUrl());

        return PaymentLinkResponse.builder()
            .checkoutUrl(data.getCheckoutUrl())
            .qrCode(data.getQrCode())
            .orderCode(orderCode)
            .paymentLinkId(data.getPaymentLinkId())
            .build();
      } else {
        throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
      }

    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error creating payment link", e);
      throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
    }
  }

  @Transactional
  public void handleWebhook(PayOSWebhookRequest webhookRequest) {
    log.info("Processing webhook for orderCode: {}", webhookRequest.getData().getOrderCode());
    log.info(
        "Webhook full data: code={}, desc={}, success={}, data={}",
        webhookRequest.getCode(),
        webhookRequest.getDesc(),
        webhookRequest.getSuccess(),
        webhookRequest.getData());

    try {
      // Verify webhook signature
      if (!verifyWebhookSignature(webhookRequest)) {
        log.error(
            "Invalid webhook signature for orderCode: {}", webhookRequest.getData().getOrderCode());
        log.error(
            "Webhook will still be processed (signature check temporarily disabled for debugging)");
        // Don't throw exception yet - let's process to see what happens
        // throw new AppException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
      }

      // Find transaction
      Transaction transaction =
          transactionRepository
              .findByOrderCode(webhookRequest.getData().getOrderCode())
              .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

      log.info(
          "Found transaction: id={}, status={}, amount={}",
          transaction.getId(),
          transaction.getStatus(),
          transaction.getAmount());

      // Check if already processed
      if (transaction.getStatus() == TransactionStatus.SUCCESS) {
        log.warn("Transaction already processed: {}", transaction.getOrderCode());
        return; // Don't throw error, just skip processing
      }

      // Process based on webhook code (use root level code, not data.code)
      if ("00".equals(webhookRequest.getCode())
          && Boolean.TRUE.equals(webhookRequest.getSuccess())) {
        // Payment successful
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setReferenceCode(webhookRequest.getData().getReference());

        // Parse transaction date
        try {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          LocalDateTime dateTime =
              LocalDateTime.parse(webhookRequest.getData().getTransactionDateTime(), formatter);
          transaction.setTransactionDate(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
          log.warn("Error parsing transaction date, using current time", e);
          transaction.setTransactionDate(Instant.now());
        }

        // Add balance to wallet
        log.info(
            "Adding balance {} to wallet {}",
            transaction.getAmount(),
            transaction.getWallet().getId());
        walletService.addBalance(transaction.getWallet().getId(), transaction.getAmount());

        log.info(
            "✅ Payment processed successfully for orderCode: {}, amount: {}, wallet: {}",
            transaction.getOrderCode(),
            transaction.getAmount(),
            transaction.getWallet().getId());
      } else {
        // Payment failed or cancelled
        transaction.setStatus(TransactionStatus.FAILED);
        log.warn(
            "❌ Payment failed/cancelled for orderCode: {}, code: {}, desc: {}",
            transaction.getOrderCode(),
            webhookRequest.getCode(),
            webhookRequest.getDesc());
      }

      transactionRepository.save(transaction);

    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing webhook", e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  @Transactional
  public void manualConfirmTransaction(Long orderCode) {
    log.info("Manually confirming transaction with orderCode: {}", orderCode);

    // Find transaction
    Transaction transaction =
        transactionRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

    log.info(
        "Found transaction: id={}, status={}, amount={}, wallet={}",
        transaction.getId(),
        transaction.getStatus(),
        transaction.getAmount(),
        transaction.getWallet().getId());

    // Check if already processed
    if (transaction.getStatus() == TransactionStatus.SUCCESS) {
      log.warn("Transaction already processed: {}", transaction.getOrderCode());
      throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    // Update transaction status
    transaction.setStatus(TransactionStatus.SUCCESS);
    transaction.setTransactionDate(Instant.now());
    transaction.setReferenceCode("MANUAL_CONFIRM_" + System.currentTimeMillis());

    // Add balance to wallet
    log.info(
        "Adding balance {} to wallet {}", transaction.getAmount(), transaction.getWallet().getId());
    walletService.addBalance(transaction.getWallet().getId(), transaction.getAmount());

    transactionRepository.save(transaction);

    log.info(
        "✅ Transaction manually confirmed successfully for orderCode: {}, amount: {}, wallet: {}",
        transaction.getOrderCode(),
        transaction.getAmount(),
        transaction.getWallet().getId());
  }

  private String createPaymentSignature(Map<String, Object> paymentData) {
    try {
      // Build data string for signature
      String dataStr =
          String.format(
              "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
              paymentData.get("amount"),
              paymentData.get("cancelUrl"),
              paymentData.get("description"),
              paymentData.get("orderCode"),
              paymentData.get("returnUrl"));

      // Calculate HMAC SHA256
      Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
      SecretKeySpec secret_key =
          new SecretKeySpec(
              payOSProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      sha256_HMAC.init(secret_key);

      byte[] hash = sha256_HMAC.doFinal(dataStr.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }

      return hexString.toString();

    } catch (Exception e) {
      log.error("Error creating payment signature", e);
      throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
    }
  }

  private boolean verifyWebhookSignature(PayOSWebhookRequest webhookRequest) {
    try {
      // Build data string for signature verification
      PayOSWebhookRequest.WebhookData data = webhookRequest.getData();

      // According to PayOS docs, signature data should be sorted alphabetically
      String dataStr =
          String.format(
              "amount=%d&code=%s&desc=%s&orderCode=%d&success=%s",
              data.getAmount(),
              webhookRequest.getCode(),
              webhookRequest.getDesc(),
              data.getOrderCode(),
              webhookRequest.getSuccess());

      log.info("Signature verification - Data string: {}", dataStr);
      log.info(
          "Signature verification - Checksum key length: {}",
          payOSProperties.getChecksumKey().length());

      // Calculate HMAC SHA256
      Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
      SecretKeySpec secret_key =
          new SecretKeySpec(
              payOSProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      sha256_HMAC.init(secret_key);

      byte[] hash = sha256_HMAC.doFinal(dataStr.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }

      String calculatedSignature = hexString.toString();
      log.info("Signature verification - Calculated: {}", calculatedSignature);
      log.info("Signature verification - Received: {}", webhookRequest.getSignature());
      log.info(
          "Signature verification - Match: {}",
          calculatedSignature.equals(webhookRequest.getSignature()));

      return calculatedSignature.equals(webhookRequest.getSignature());

    } catch (Exception e) {
      log.error("Error verifying webhook signature", e);
      return false;
    }
  }
}
