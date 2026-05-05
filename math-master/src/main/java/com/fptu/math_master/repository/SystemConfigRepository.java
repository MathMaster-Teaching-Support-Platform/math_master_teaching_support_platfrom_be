package com.fptu.math_master.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fptu.math_master.entity.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    Optional<SystemConfig> findByConfigKeyAndDeletedAtIsNull(String configKey);

    List<SystemConfig> findAllByDeletedAtIsNull();
}
