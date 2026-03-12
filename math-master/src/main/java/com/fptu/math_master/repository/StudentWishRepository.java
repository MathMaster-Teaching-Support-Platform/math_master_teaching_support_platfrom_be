package com.fptu.math_master.repository;

import com.fptu.math_master.entity.StudentWish;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentWishRepository extends JpaRepository<StudentWish, UUID> {

  /**
   * Find active wishes by student ID and subject
   */
  @Query(
      "SELECT sw FROM StudentWish sw WHERE sw.studentId = :studentId AND sw.subject = :subject AND sw.isActive = true AND sw.deletedAt IS NULL")
  Optional<StudentWish> findActiveWishByStudentAndSubject(
      @Param("studentId") UUID studentId, @Param("subject") String subject);

  /**
   * Find all active wishes for a student
   */
  @Query(
      "SELECT sw FROM StudentWish sw WHERE sw.studentId = :studentId AND sw.isActive = true AND sw.deletedAt IS NULL")
  List<StudentWish> findAllActiveWishesByStudent(@Param("studentId") UUID studentId);

  /**
   * Find all wishes for a student (including inactive)
   */
  @Query(
      "SELECT sw FROM StudentWish sw WHERE sw.studentId = :studentId AND sw.deletedAt IS NULL ORDER BY sw.createdAt DESC")
  List<StudentWish> findAllWishesByStudent(@Param("studentId") UUID studentId);

  /**
   * Find active wishes by subject
   */
  @Query(
      "SELECT sw FROM StudentWish sw WHERE sw.subject = :subject AND sw.isActive = true AND sw.deletedAt IS NULL")
  List<StudentWish> findActiveWishesBySubject(@Param("subject") String subject);

  /**
   * Check if wish exists for student and subject
   */
  @Query(
      "SELECT CASE WHEN COUNT(sw) > 0 THEN true ELSE false END FROM StudentWish sw WHERE sw.studentId = :studentId AND sw.subject = :subject AND sw.deletedAt IS NULL")
  boolean existsByStudentAndSubject(
      @Param("studentId") UUID studentId, @Param("subject") String subject);
}
