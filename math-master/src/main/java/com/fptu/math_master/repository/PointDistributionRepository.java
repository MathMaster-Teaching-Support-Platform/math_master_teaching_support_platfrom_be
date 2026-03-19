package com.fptu.math_master.repository;

import com.fptu.math_master.entity.PointDistribution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PointDistributionRepository extends JpaRepository<PointDistribution, UUID> {

  /** Delete all cached distribution rows for a matrix before recomputing. */
  @Modifying
  @Query("DELETE FROM PointDistribution pd WHERE pd.matrixId = :matrixId")
  void deleteByMatrixId(@Param("matrixId") UUID matrixId);
}
