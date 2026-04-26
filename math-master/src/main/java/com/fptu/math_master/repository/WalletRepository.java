package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  @Modifying
  @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.updatedAt = CURRENT_TIMESTAMP WHERE w.id = :id")
  void incrementBalance(@Param("id") UUID id, @Param("amount") java.math.BigDecimal amount);

  @Modifying
  @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.updatedAt = CURRENT_TIMESTAMP WHERE w.id = :id AND w.balance >= :amount")
  int decrementBalance(@Param("id") UUID id, @Param("amount") java.math.BigDecimal amount);

  Optional<Wallet> findByUserId(UUID userId);

  // FIX #4: Add pessimistic locking for wallet operations to prevent race conditions
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
  Optional<Wallet> findByUserIdWithLock(@Param("userId") UUID userId);

  boolean existsByUserId(UUID userId);
}