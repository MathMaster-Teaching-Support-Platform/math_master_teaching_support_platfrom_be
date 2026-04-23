package com.fptu.math_master.repository;

import com.fptu.math_master.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findAllByUser_Id(UUID userId);

    Optional<NotificationPreference> findByUser_IdAndNotificationType(UUID userId, String notificationType);

    @Query("SELECT np FROM NotificationPreference np WHERE np.user.id = :userId AND np.notificationType = :notificationType AND np.pushEnabled = true")
    Optional<NotificationPreference> findByUserIdAndTypeWithPushEnabled(@Param("userId") UUID userId, @Param("notificationType") String notificationType);

    @Query("SELECT np FROM NotificationPreference np WHERE np.user.id = :userId AND np.notificationType = :notificationType AND np.inAppEnabled = true")
    Optional<NotificationPreference> findByUserIdAndTypeWithInAppEnabled(@Param("userId") UUID userId, @Param("notificationType") String notificationType);
}