package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Permission;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
  Optional<Permission> findByCode(String code);

  boolean existsByCode(String code);

  Set<Permission> findByCodeIn(Set<String> codes);
}
