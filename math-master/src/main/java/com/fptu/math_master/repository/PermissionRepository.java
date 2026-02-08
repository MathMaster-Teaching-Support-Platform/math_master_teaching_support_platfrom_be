package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
  Optional<Permission> findByCode(String code);

  boolean existsByCode(String code);

  Set<Permission> findByCodeIn(Set<String> codes);
}
