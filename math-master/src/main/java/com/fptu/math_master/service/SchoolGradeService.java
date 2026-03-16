package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateSchoolGradeRequest;
import com.fptu.math_master.dto.request.UpdateSchoolGradeRequest;
import com.fptu.math_master.dto.response.SchoolGradeResponse;
import java.util.List;
import java.util.UUID;

public interface SchoolGradeService {

  SchoolGradeResponse create(CreateSchoolGradeRequest request);

  SchoolGradeResponse update(UUID id, UpdateSchoolGradeRequest request);

  SchoolGradeResponse getById(UUID id);

  List<SchoolGradeResponse> getAll(boolean activeOnly);

  void deactivate(UUID id);
}
