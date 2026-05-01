package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrixPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamMatrixPartRepository extends JpaRepository<ExamMatrixPart, UUID> {

    List<ExamMatrixPart> findByExamMatrixIdOrderByPartNumber(UUID examMatrixId);

    Optional<ExamMatrixPart> findByExamMatrixIdAndPartNumber(UUID examMatrixId, Integer partNumber);

    @Modifying
    @Query("DELETE FROM ExamMatrixPart p WHERE p.examMatrixId = :examMatrixId")
    void deleteByExamMatrixId(@Param("examMatrixId") UUID examMatrixId);
}
