package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    Optional<Wallet> findByUserId(Integer userId);
    
    boolean existsByUserId(Integer userId);
}
