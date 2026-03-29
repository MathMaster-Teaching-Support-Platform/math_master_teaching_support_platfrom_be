package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ChatSession;
import com.fptu.math_master.enums.ChatSessionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

  @Query("SELECT s FROM ChatSession s WHERE s.id = :id AND s.deletedAt IS NULL")
  Optional<ChatSession> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT s FROM ChatSession s "
          + "WHERE s.userId = :userId AND s.deletedAt IS NULL "
          + "AND (:status IS NULL OR s.status = :status) "
          + "AND (:keywordPattern IS NULL OR LOWER(s.title) LIKE :keywordPattern) "
          + "ORDER BY s.lastMessageAt DESC, s.createdAt DESC")
  Page<ChatSession> findByUserAndFilters(
      @Param("userId") UUID userId,
      @Param("status") ChatSessionStatus status,
      @Param("keywordPattern") String keywordPattern,
      Pageable pageable);
}
