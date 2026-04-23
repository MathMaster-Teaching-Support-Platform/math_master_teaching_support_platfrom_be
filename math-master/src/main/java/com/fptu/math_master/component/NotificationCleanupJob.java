package com.fptu.math_master.component;

import com.fptu.math_master.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    /**
     * Runs daily at 2 AM to clean up old notifications
     * Deletes notifications older than 90 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            Instant cutoffDate = Instant.now().minus(90, ChronoUnit.DAYS);
            
            long deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} old notifications (older than 90 days)", deletedCount);
            } else {
                log.debug("No old notifications to clean up");
            }
        } catch (Exception e) {
            log.error("Failed to clean up old notifications", e);
        }
    }

    /**
     * Runs weekly on Sunday at 3 AM to clean up orphaned notifications
     * Deletes notifications for users that no longer exist
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupOrphanedNotifications() {
        try {
            long deletedCount = notificationRepository.deleteOrphanedNotifications();
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} orphaned notifications", deletedCount);
            } else {
                log.debug("No orphaned notifications to clean up");
            }
        } catch (Exception e) {
            log.error("Failed to clean up orphaned notifications", e);
        }
    }
}