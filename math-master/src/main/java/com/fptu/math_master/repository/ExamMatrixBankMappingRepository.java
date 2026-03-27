package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrixBankMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamMatrixBankMappingRepository extends JpaRepository<ExamMatrixBankMapping, UUID> {

  @Query(
      "SELECT m FROM ExamMatrixBankMapping m WHERE m.examMatrixId = :matrixId ORDER BY m.createdAt")
  List<ExamMatrixBankMapping> findByExamMatrixIdOrderByCreatedAt(@Param("matrixId") UUID matrixId);

  @Query("SELECT m FROM ExamMatrixBankMapping m WHERE m.id = :id AND m.examMatrixId = :matrixId")
  Optional<ExamMatrixBankMapping> findByIdAndExamMatrixId(
      @Param("id") UUID id, @Param("matrixId") UUID matrixId);
}
