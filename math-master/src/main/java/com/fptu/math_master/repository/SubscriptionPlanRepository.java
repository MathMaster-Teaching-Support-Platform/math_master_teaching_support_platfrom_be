package com.fptu.math_master.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.SubscriptionPlan;
import com.fptu.math_master.enums.SubscriptionPlanStatus;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  Optional<SubscriptionPlan> findBySlug(String slug);

  @Query("SELECT p FROM SubscriptionPlan p WHERE p.deletedAt IS NULL ORDER BY p.createdAt ASC")
  List<SubscriptionPlan> findAllActive();

  @Query("SELECT p FROM SubscriptionPlan p WHERE p.deletedAt IS NULL AND p.status = :status ORDER BY p.createdAt ASC")
  List<SubscriptionPlan> findAllByStatus(SubscriptionPlanStatus status);
}
