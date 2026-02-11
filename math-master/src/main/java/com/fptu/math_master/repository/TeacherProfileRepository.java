package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.ProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

  Optional<TeacherProfile> findByUser(User user);

  Optional<TeacherProfile> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);

  Page<TeacherProfile> findByStatus(ProfileStatus status, Pageable pageable);

  @Query("SELECT tp FROM TeacherProfile tp WHERE tp.status = :status ORDER BY tp.createdAt DESC")
  Page<TeacherProfile> findByStatusOrderByCreatedAtDesc(ProfileStatus status, Pageable pageable);

  @Query("SELECT COUNT(tp) FROM TeacherProfile tp WHERE tp.status = 'PENDING'")
  long countPendingProfiles();
}
