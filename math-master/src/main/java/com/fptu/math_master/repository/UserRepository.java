package com.fptu.math_master.repository;

import java.util.Optional;
import java.util.UUID;

import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {
  boolean existsByUserName(String userName);

  Optional<User> findByUserName(String userName);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<User> findByIdWithRoles(UUID id);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.userName = :userName")
  Optional<User> findByUserNameWithRoles(String userName);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
  Optional<User> findByEmailWithRolesAndPermissions(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
  Optional<User> findByIdWithRolesAndPermissions(UUID id);

  List<User> findByStatus(Status status);

  Page<User> findByStatus(Status status, Pageable pageable);
}
