package com.fptu.math_master.repository;

import com.fptu.math_master.entity.FeedbackAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, UUID> {
  List<FeedbackAttachment> findAllByFeedback_IdOrderByCreatedAtAsc(UUID feedbackId);
}
