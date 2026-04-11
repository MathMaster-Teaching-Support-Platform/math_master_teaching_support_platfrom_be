package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TeachingResource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeachingResourceRepository extends JpaRepository<TeachingResource, UUID> {

  @Query("SELECT tr FROM TeachingResource tr WHERE tr.id = :id AND tr.deletedAt IS NULL")
  Optional<TeachingResource> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT DISTINCT tr FROM TeachingResource tr "
          + "JOIN tr.roadmapTopics rt "
          + "WHERE rt.id = :topicId AND tr.deletedAt IS NULL")
  List<TeachingResource> findByRoadmapTopicId(@Param("topicId") UUID topicId);
}
