package com.fptu.math_master.component;

import com.fptu.math_master.entity.WithdrawalRequest;
import com.fptu.math_master.enums.WithdrawalStatus;
import com.fptu.math_master.repository.WithdrawalRequestRepository;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler that auto-cancels PENDING_VERIFY withdrawal requests whose OTP has expired.
 * Runs every 5 minutes.
 *
 * <p>Invariant: Only PENDING_VERIFY requests with {@code otpExpiry < now} are cancelled.
 * PENDING_ADMIN requests are never auto-cancelled — admins must manually reject them.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WithdrawalOtpCancelScheduler {

  WithdrawalRequestRepository withdrawalRequestRepository;

  @Scheduled(fixedDelay = 300_000) // every 5 minutes
  @Transactional
  public void cancelExpiredOtpRequests() {
    Instant now = Instant.now();
    List<WithdrawalRequest> expired = withdrawalRequestRepository.findExpiredPendingVerify(now);

    if (expired.isEmpty()) {
      return;
    }

    log.info("Auto-cancelling {} expired PENDING_VERIFY withdrawal request(s)", expired.size());

    for (WithdrawalRequest wr : expired) {
      wr.setStatus(WithdrawalStatus.CANCELLED);
      wr.setOtpCode(null);
      wr.setOtpExpiry(null);
      log.info(
          "Cancelled expired PENDING_VERIFY withdrawal request: id={}, userId={}, otpExpiry={}",
          wr.getId(),
          wr.getUser().getId(),
          wr.getOtpExpiry());
    }
  }
}
