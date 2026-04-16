package com.fptu.math_master.component;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.repository.TransactionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler that auto-cancels PENDING deposit transactions that have passed their expiresAt time
 * (15 minutes after creation). Runs every 60 seconds.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PendingTransactionCancelScheduler {

  TransactionRepository transactionRepository;

  @Scheduled(fixedDelay = 60_000)
  @Transactional
  public void cancelExpiredPendingTransactions() {
    Instant now = Instant.now();
    List<Transaction> expired = transactionRepository.findExpiredPendingTransactions(now);

    if (expired.isEmpty()) {
      return;
    }

    log.info("Auto-cancelling {} expired PENDING transaction(s)", expired.size());

    for (Transaction tx : expired) {
      tx.setStatus(TransactionStatus.CANCELLED);
      log.info(
          "Cancelled expired PENDING transaction: orderCode={}, walletId={}, expiresAt={}",
          tx.getOrderCode(),
          tx.getWallet().getId(),
          tx.getExpiresAt());
    }

    transactionRepository.saveAll(expired);
  }
}
