package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

  Optional<Transaction> findByOrderCode(Long orderCode);

  Page<Transaction> findByWalletIdAndStatus(
      UUID walletId, TransactionStatus status, Pageable pageable);
}
