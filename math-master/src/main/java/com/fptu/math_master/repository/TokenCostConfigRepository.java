package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TokenCostConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenCostConfigRepository extends JpaRepository<TokenCostConfig, UUID> {
  Optional<TokenCostConfig> findByFeatureKey(String featureKey);
}
