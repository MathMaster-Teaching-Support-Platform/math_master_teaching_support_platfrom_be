package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.TeachingResourceResponse;
import java.util.List;
import java.util.UUID;

public interface RoadmapTopicResourceService {

  List<TeachingResourceResponse> attachResources(UUID topicId, List<UUID> resourceIds);

  void removeResource(UUID topicId, UUID resourceId);

  List<TeachingResourceResponse> getResourcesOfTopic(UUID topicId);
}
