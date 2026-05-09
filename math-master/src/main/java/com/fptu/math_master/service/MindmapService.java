package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.GenerateMindmapRequest;
import com.fptu.math_master.dto.request.MindmapNodeRequest;
import com.fptu.math_master.dto.request.MindmapRequest;
import com.fptu.math_master.dto.response.MindmapDetailResponse;
import com.fptu.math_master.dto.response.MindmapNodeResponse;
import com.fptu.math_master.dto.response.MindmapResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MindmapService {

  /**
   * Create a new mindmap
   *
   * @param request mindmap creation request
   * @return created mindmap response
   */
  MindmapResponse createMindmap(MindmapRequest request);

  /**
   * Generate a mindmap using AI
   *
   * @param request generation request with prompt
   * @return generated mindmap with nodes
   */
  MindmapDetailResponse generateMindmap(GenerateMindmapRequest request);

  /**
   * Get mindmap by id with all nodes
   *
   * @param id mindmap id
   * @return mindmap detail with nodes
   */
  MindmapDetailResponse getMindmapById(UUID id);

  MindmapDetailResponse getPublicMindmapById(UUID id);

  /**
   * Update mindmap
   *
   * @param id mindmap id
   * @param request update request
   * @return updated mindmap
   */
  MindmapResponse updateMindmap(UUID id, MindmapRequest request);

  /**
   * Delete mindmap (soft delete)
   *
   * @param id mindmap id
   */
  void deleteMindmap(UUID id);

  /**
   * Publish mindmap
   *
   * @param id mindmap id
   * @return published mindmap
   */
  MindmapResponse publishMindmap(UUID id);

  /**
   * Archive mindmap
   *
   * @param id mindmap id
   * @return archived mindmap
   */
  MindmapResponse archiveMindmap(UUID id);

  /**
   * Get all mindmaps by current teacher
   *
   * @param lessonId optional lesson id filter
   * @param pageable pagination
   * @return page of mindmaps
   */
  Page<MindmapResponse> getMyMindmaps(
      UUID gradeId, UUID subjectId, UUID chapterId, UUID lessonId, Pageable pageable);

  /**
   * Get mindmaps by lesson id
   *
   * @param lessonId lesson id
   * @param pageable pagination
   * @return page of mindmaps
   */
  Page<MindmapResponse> getMindmapsByLesson(UUID lessonId, Pageable pageable);

  /**
   * Get all public (published) mindmaps.
   *
   * @param pageable pagination
   * @return page of public mindmaps
   */
  Page<MindmapResponse> getPublicMindmaps(
      UUID gradeId,
      UUID subjectId,
      UUID chapterId,
      UUID lessonId,
      String name,
      Pageable pageable);

  BinaryFileData exportMindmap(UUID id, String format);

  BinaryFileData exportPublicMindmap(UUID id, String format);

  /**
   * Create a node in mindmap
   *
   * @param request node creation request
   * @return created node
   */
  MindmapNodeResponse createNode(MindmapNodeRequest request);

  /**
   * Update a node
   *
   * @param nodeId node id
   * @param request update request
   * @return updated node
   */
  MindmapNodeResponse updateNode(UUID nodeId, MindmapNodeRequest request);

  /**
   * Delete a node and its children
   *
   * @param nodeId node id
   */
  void deleteNode(UUID nodeId);

  /**
   * Get all nodes of a mindmap
   *
   * @param mindmapId mindmap id
   * @return list of nodes with hierarchical structure
   */
  List<MindmapNodeResponse> getNodesByMindmap(UUID mindmapId);

  List<MindmapNodeResponse> getPublicNodesByMindmap(UUID mindmapId);

  record BinaryFileData(byte[] content, String fileName, String contentType) {}
}
