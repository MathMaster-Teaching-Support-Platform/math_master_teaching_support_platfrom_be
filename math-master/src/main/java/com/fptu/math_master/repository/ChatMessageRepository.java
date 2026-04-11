package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

  Page<ChatMessage> findBySessionIdAndDeletedAtIsNull(UUID sessionId, Pageable pageable);

  List<ChatMessage> findAllBySessionIdAndDeletedAtIsNull(UUID sessionId);

  @Query(
      "SELECT COALESCE(MAX(m.sequenceNo), 0) FROM ChatMessage m "
          + "WHERE m.sessionId = :sessionId AND m.deletedAt IS NULL")
  long findMaxSequenceNoBySessionId(@Param("sessionId") UUID sessionId);
}
