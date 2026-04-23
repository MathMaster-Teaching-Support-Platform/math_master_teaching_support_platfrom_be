package com.fptu.math_master.repository;

import com.fptu.math_master.entity.UserFcmToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, UUID> {

  Optional<UserFcmToken> findByToken(String token);

  List<UserFcmToken> findAllByUser_IdAndIsActiveTrue(UUID userId);

  List<UserFcmToken> findAllByIsActiveTrue();
}
