package com.fptu.math_master.repository;

import com.fptu.math_master.entity.StudentRoadmapProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRoadmapProgressRepository extends JpaRepository<StudentRoadmapProgress, UUID> {

  Optional<StudentRoadmapProgress> findByStudentIdAndRoadmapId(UUID studentId, UUID roadmapId);

  List<StudentRoadmapProgress> findByStudentId(UUID studentId);
}
