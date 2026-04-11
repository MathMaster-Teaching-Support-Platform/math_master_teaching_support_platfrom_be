package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.EnrollmentResponse;
import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

  EnrollmentResponse enroll(UUID courseId);

  EnrollmentResponse drop(UUID enrollmentId);

  List<EnrollmentResponse> getMyEnrollments();
}
