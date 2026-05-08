package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Feedback;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
  Page<Feedback> findAllBySender_IdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);
}
