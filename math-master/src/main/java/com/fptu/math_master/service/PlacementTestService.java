package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.SubmitPlacementTestRequest;
import com.fptu.math_master.dto.response.StudentRoadmapProgressResponse;
import java.util.UUID;

public interface PlacementTestService {

  StudentRoadmapProgressResponse submitPlacementTest(
      UUID studentId, SubmitPlacementTestRequest request);
}
