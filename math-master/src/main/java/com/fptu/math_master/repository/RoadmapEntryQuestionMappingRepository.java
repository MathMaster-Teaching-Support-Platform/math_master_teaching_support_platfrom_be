package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RoadmapEntryQuestionMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadmapEntryQuestionMappingRepository
    extends JpaRepository<RoadmapEntryQuestionMapping, UUID> {

  List<RoadmapEntryQuestionMapping> findByRoadmapIdOrderByOrderIndex(UUID roadmapId);

  void deleteByRoadmapId(UUID roadmapId);

  void deleteByRoadmapTopicId(UUID roadmapTopicId);
}
