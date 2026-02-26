package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.StudentWishRequest;
import com.fptu.math_master.dto.response.StudentWishResponse;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing student learning wishes and preferences
 */
public interface StudentWishService {

  /**
   * Create or update a student's learning wish for a subject
   *
   * @param studentId Student identifier
   * @param request Student wish details
   * @return Created/updated wish response
   */
  StudentWishResponse upsertWish(UUID studentId, StudentWishRequest request);

  /**
   * Get active wish for a student and subject
   *
   * @param studentId Student identifier
   * @param subject Subject name
   * @return Student wish response
   */
  StudentWishResponse getActiveWish(UUID studentId, String subject);

  /**
   * Get all active wishes for a student
   *
   * @param studentId Student identifier
   * @return List of active wishes
   */
  List<StudentWishResponse> getActiveWishes(UUID studentId);

  /**
   * Get all wishes for a student (including inactive)
   *
   * @param studentId Student identifier
   * @return List of all wishes
   */
  List<StudentWishResponse> getAllWishes(UUID studentId);

  /**
   * Deactivate a wish
   *
   * @param wishId Wish identifier
   */
  void deactivateWish(UUID wishId);

  /**
   * Delete a wish
   *
   * @param wishId Wish identifier
   */
  void deleteWish(UUID wishId);

  /**
   * Check if wish exists for student and subject
   *
   * @param studentId Student identifier
   * @param subject Subject name
   * @return true if wish exists
   */
  boolean wishExists(UUID studentId, String subject);
}
