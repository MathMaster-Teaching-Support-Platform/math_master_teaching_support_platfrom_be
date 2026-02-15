package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Wallet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  Optional<Wallet> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);
}
