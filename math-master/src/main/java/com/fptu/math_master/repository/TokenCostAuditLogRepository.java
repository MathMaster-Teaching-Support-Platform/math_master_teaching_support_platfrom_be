package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TokenCostAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenCostAuditLogRepository extends JpaRepository<TokenCostAuditLog, UUID> {
  List<TokenCostAuditLog> findAllByOrderByCreatedAtDesc();
}
