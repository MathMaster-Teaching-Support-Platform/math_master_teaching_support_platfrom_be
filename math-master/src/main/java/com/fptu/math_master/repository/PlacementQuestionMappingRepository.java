package com.fptu.math_master.repository;

import com.fptu.math_master.entity.PlacementQuestionMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementQuestionMappingRepository extends JpaRepository<PlacementQuestionMapping, UUID> {

  List<PlacementQuestionMapping> findByPlacementAssessmentIdOrderByOrderIndex(UUID placementAssessmentId);

  void deleteByPlacementAssessmentId(UUID placementAssessmentId);
}
