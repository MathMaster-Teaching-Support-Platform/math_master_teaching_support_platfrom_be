package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateCurriculumRequest;
import com.fptu.math_master.dto.request.UpdateCurriculumRequest;
import com.fptu.math_master.dto.response.CurriculumResponse;
import com.fptu.math_master.enums.CurriculumCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurriculumService {
  /**
   * Create a new curriculum
   *
   * @param request creation request
   * @return created curriculum
   */
  CurriculumResponse createCurriculum(CreateCurriculumRequest request);

  /**
   * Update an existing curriculum
   *
   * @param curriculumId curriculum ID
   * @param request update request
   * @return updated curriculum
   */
  CurriculumResponse updateCurriculum(UUID curriculumId, UpdateCurriculumRequest request);

  /**
   * Get curriculum by ID
   *
   * @param curriculumId curriculum ID
   * @return curriculum details
   */
  CurriculumResponse getCurriculumById(UUID curriculumId);

  /**
   * Get all curricula (paginated)
   *
   * @param pageable pagination info
   * @return paginated curricula
   */
  Page<CurriculumResponse> getAllCurricula(Pageable pageable);

  /**
   * Get all curricula (no pagination)
   *
   * @return list of all curricula
   */
  List<CurriculumResponse> getAllCurricula();

  /**
   * Get curricula by grade
   *
   * @param grade grade level
   * @return list of curricula for grade
   */
  List<CurriculumResponse> getCurriculaByGrade(Integer grade);

  /**
   * Get curricula by category
   *
   * @param category curriculum category
   * @return list of curricula for category
   */
  List<CurriculumResponse> getCurriculaByCategory(CurriculumCategory category);

  /**
   * Get curricula by grade and category
   *
   * @param grade grade level
   * @param category curriculum category
   * @return list of curricula
   */
  List<CurriculumResponse> getCurriculaByGradeAndCategory(
      Integer grade, CurriculumCategory category);

  /**
   * Delete a curriculum (soft delete)
   *
   * @param curriculumId curriculum ID
   */
  void deleteCurriculum(UUID curriculumId);

  /**
   * Search curricula by name
   *
   * @param name name to search
   * @return list of matching curricula
   */
  List<CurriculumResponse> searchCurriculaByName(String name);
}
