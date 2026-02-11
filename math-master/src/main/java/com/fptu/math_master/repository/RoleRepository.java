package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByName(String name);

  boolean existsByName(String name);

  @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
  Optional<Role> findByIdWithPermissions(UUID id);
}
