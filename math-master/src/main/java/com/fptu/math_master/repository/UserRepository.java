package com.fptu.math_master.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  boolean existsByUserName(String userName);

  Optional<User> findByUserName(String userName);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<User> findByIdWithRoles(UUID id);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.userName = :userName")
  Optional<User> findByUserNameWithRoles(String userName);

  @Query(
      "SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
  Optional<User> findByEmailWithRolesAndPermissions(String email);

  @Query(
      "SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
  Optional<User> findByIdWithRolesAndPermissions(UUID id);

  List<User> findByStatus(Status status);

  Page<User> findByStatus(Status status, Pageable pageable);

  long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

  long countByStatus(Status status);

  Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.status != com.fptu.math_master.enums.Status.DELETED")
  long countByRoleName(String roleName);

  @Query("SELECT COUNT(u) FROM User u WHERE u.status != com.fptu.math_master.enums.Status.DELETED")
  long countNonDeleted();
}
