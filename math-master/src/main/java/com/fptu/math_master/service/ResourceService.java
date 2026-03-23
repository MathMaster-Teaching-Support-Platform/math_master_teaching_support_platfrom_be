package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.TeachingResourceResponse;
import com.fptu.math_master.enums.TeachingResourceType;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ResourceService {

  TeachingResourceResponse uploadResource(String name, TeachingResourceType type, MultipartFile file);

  TeachingResourceResponse getResource(UUID resourceId);

  void deleteResource(UUID resourceId);
}
