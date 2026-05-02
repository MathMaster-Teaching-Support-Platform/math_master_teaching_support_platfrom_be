package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.enums.CognitiveLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  @Query(
      "SELECT m FROM ExamMatrixBankMapping m "
          + "WHERE m.examMatrixId = :matrixId AND m.matrixRowId = :rowId "
          + "ORDER BY m.createdAt")
  List<ExamMatrixBankMapping> findByExamMatrixIdAndMatrixRowIdOrderByCreatedAt(
      @Param("matrixId") UUID matrixId, @Param("rowId") UUID rowId);

  @Query(
      "SELECT m FROM ExamMatrixBankMapping m "
          + "WHERE m.examMatrixId = :matrixId AND m.matrixRowId = :rowId "
          + "AND m.cognitiveLevel = :cognitiveLevel")
  Optional<ExamMatrixBankMapping> findByExamMatrixIdAndMatrixRowIdAndCognitiveLevel(
      @Param("matrixId") UUID matrixId,
      @Param("rowId") UUID rowId,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel);

  @Modifying
  @Query(
      "DELETE FROM ExamMatrixBankMapping m "
          + "WHERE m.examMatrixId = :matrixId AND m.matrixRowId = :rowId")
  void deleteByExamMatrixIdAndMatrixRowId(
      @Param("matrixId") UUID matrixId, @Param("rowId") UUID rowId);

  // BE-5: Delete cells when reducing number of parts
  @Modifying
  @Query(
      "DELETE FROM ExamMatrixBankMapping e "
          + "WHERE e.examMatrixId = :matrixId AND e.partNumber > :partNumber")
  void deleteByExamMatrixIdAndPartNumberGreaterThan(
      @Param("matrixId") UUID matrixId, @Param("partNumber") Integer partNumber);

  // BE-7: Find cell by unique key for upsert
  @Query(
      "SELECT m FROM ExamMatrixBankMapping m "
          + "WHERE m.examMatrixId = :matrixId AND m.matrixRowId = :rowId "
          + "AND m.partNumber = :partNumber AND m.cognitiveLevel = :cognitiveLevel")
  Optional<ExamMatrixBankMapping> findByExamMatrixIdAndMatrixRowIdAndPartNumberAndCognitiveLevel(
      @Param("matrixId") UUID matrixId,
      @Param("rowId") UUID rowId,
      @Param("partNumber") Integer partNumber,
      @Param("cognitiveLevel") CognitiveLevel cognitiveLevel);

  // Delete all bank mappings for a specific row (used when deleting a row)
  @Modifying
  @Query("DELETE FROM ExamMatrixBankMapping m WHERE m.matrixRowId = :rowId")
  void deleteByMatrixRowId(@Param("rowId") UUID rowId);
}
