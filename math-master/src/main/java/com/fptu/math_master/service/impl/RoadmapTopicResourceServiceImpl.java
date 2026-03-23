package com.fptu.math_master.service.impl;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.response.TeachingResourceResponse;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.TeachingResource;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.TeachingResourceRepository;
import com.fptu.math_master.service.RoadmapTopicResourceService;
import com.fptu.math_master.util.SecurityUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class RoadmapTopicResourceServiceImpl implements RoadmapTopicResourceService {

  RoadmapTopicRepository roadmapTopicRepository;
  TeachingResourceRepository teachingResourceRepository;
  LearningRoadmapRepository learningRoadmapRepository;

  @Override
  public List<TeachingResourceResponse> attachResources(UUID topicId, List<UUID> resourceIds) {
    RoadmapTopic topic = getActiveTopic(topicId);
    validateTopicWriteAccess(topic);

    Set<TeachingResource> attachments =
        topic.getTeachingResources() == null ? new HashSet<>() : new HashSet<>(topic.getTeachingResources());

    for (UUID resourceId : resourceIds) {
      if (resourceId == null) {
        continue;
      }
      TeachingResource resource = getActiveResource(resourceId);
      attachments.add(resource);
    }

    topic.setTeachingResources(attachments);
    RoadmapTopic saved = roadmapTopicRepository.save(topic);
    return saved.getTeachingResources().stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  @Override
  public void removeResource(UUID topicId, UUID resourceId) {
    RoadmapTopic topic = getActiveTopic(topicId);
    validateTopicWriteAccess(topic);

    TeachingResource resource = getActiveResource(resourceId);
    if (topic.getTeachingResources() != null) {
      topic.getTeachingResources().remove(resource);
      roadmapTopicRepository.save(topic);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeachingResourceResponse> getResourcesOfTopic(UUID topicId) {
    RoadmapTopic topic = getActiveTopic(topicId);
    validateTopicReadAccess(topic);

    return teachingResourceRepository.findByRoadmapTopicId(topicId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private RoadmapTopic getActiveTopic(UUID topicId) {
    RoadmapTopic topic =
        roadmapTopicRepository
            .findById(topicId)
            .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_TOPIC_NOT_FOUND));
    if (topic.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ROADMAP_TOPIC_NOT_FOUND);
    }
    return topic;
  }

  private TeachingResource getActiveResource(UUID resourceId) {
    return teachingResourceRepository
        .findByIdAndNotDeleted(resourceId)
        .orElseThrow(() -> new AppException(ErrorCode.TEACHING_RESOURCE_NOT_FOUND));
  }

  private void validateTopicWriteAccess(RoadmapTopic topic) {
    LearningRoadmap roadmap =
        learningRoadmapRepository
            .findById(topic.getRoadmapId())
            .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_TOPIC_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean isAdmin = SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE);
    boolean isTeacherOwner = roadmap.getTeacherId() != null && roadmap.getTeacherId().equals(currentUserId);

    if (!isAdmin && !isTeacherOwner) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
  }

  private void validateTopicReadAccess(RoadmapTopic topic) {
    LearningRoadmap roadmap =
        learningRoadmapRepository
            .findById(topic.getRoadmapId())
            .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_TOPIC_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean isAdmin = SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE);
    boolean isTeacherOwner = roadmap.getTeacherId() != null && roadmap.getTeacherId().equals(currentUserId);
    boolean isAssignedStudent = roadmap.getStudentId() != null && roadmap.getStudentId().equals(currentUserId);

    if (!isAdmin && !isTeacherOwner && !isAssignedStudent) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
  }

  private TeachingResourceResponse mapToResponse(TeachingResource resource) {
    return TeachingResourceResponse.builder()
        .id(resource.getId())
        .name(resource.getName())
        .type(resource.getType())
        .fileUrl(resource.getFileUrl())
        .createdBy(resource.getCreatedBy())
        .createdAt(resource.getCreatedAt())
        .build();
  }
}
