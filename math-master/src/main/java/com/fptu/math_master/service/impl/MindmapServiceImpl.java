package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.GenerateMindmapRequest;
import com.fptu.math_master.dto.request.MindmapNodeRequest;
import com.fptu.math_master.dto.request.MindmapRequest;
import com.fptu.math_master.dto.response.MindmapDetailResponse;
import com.fptu.math_master.dto.response.MindmapNodeResponse;
import com.fptu.math_master.dto.response.MindmapResponse;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.MindmapStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.MindmapService;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MindmapServiceImpl implements MindmapService {

  MindmapRepository mindmapRepository;
  MindmapNodeRepository mindmapNodeRepository;
  UserRepository userRepository;
  LessonRepository lessonRepository;
  GeminiService geminiService;
  ObjectMapper objectMapper;

  @Override
  @Transactional
  public MindmapResponse createMindmap(MindmapRequest request) {
    log.info("Creating mindmap: {}", request.getTitle());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    if (request.getLessonId() != null) {
      lessonRepository
          .findById(request.getLessonId())
          .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    }

    Mindmap mindmap =
        Mindmap.builder()
            .teacherId(currentUserId)
            .lessonId(request.getLessonId())
            .title(request.getTitle())
            .description(request.getDescription())
            .aiGenerated(false)
            .status(MindmapStatus.DRAFT)
            .build();

    mindmap = mindmapRepository.save(mindmap);

    log.info("Mindmap created successfully with id: {}", mindmap.getId());
    return mapToResponse(mindmap);
  }

  @Override
  @Transactional
  public MindmapDetailResponse generateMindmap(GenerateMindmapRequest request) {
    log.info("Generating mindmap from AI prompt");

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    // Build the AI prompt with levels
    Integer levels = request.getLevels() != null ? request.getLevels() : 3;
    String aiPrompt = buildMindmapGenerationPrompt(request.getPrompt(), levels);

    // Call Gemini AI to generate mindmap structure
    String aiResponse;
    try {
      aiResponse = geminiService.sendMessage(aiPrompt);
      log.info("Received AI response for mindmap generation");
    } catch (Exception e) {
      log.error("Failed to generate mindmap from AI", e);
      throw new AppException(ErrorCode.MINDMAP_GENERATION_FAILED);
    }

    // Parse AI response to create mindmap structure
    MindmapStructure structure;
    try {
      structure = parseMindmapFromAI(aiResponse);
    } catch (Exception e) {
      log.error("Failed to parse AI response", e);
      throw new AppException(ErrorCode.INVALID_MINDMAP_STRUCTURE);
    }

    // Create mindmap entity
    UUID lessonId = null;
    if (request.getLessonId() != null && !request.getLessonId().trim().isEmpty()) {
      try {
        lessonId = UUID.fromString(request.getLessonId());
        lessonRepository
            .findById(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid lessonId format: {}", request.getLessonId());
        // lessonId remains null, mindmap will be created without lesson link
      }
    }

    Mindmap mindmap =
        Mindmap.builder()
            .teacherId(currentUserId)
            .lessonId(lessonId)
            .title(request.getTitle() != null ? request.getTitle() : structure.getTitle())
            .description(structure.getDescription())
            .aiGenerated(true)
            .generationPrompt(request.getPrompt())
            .status(MindmapStatus.DRAFT)
            .build();

    mindmap = mindmapRepository.save(mindmap);

    // Create nodes from the structure
    List<MindmapNode> nodes = createNodesFromStructure(mindmap.getId(), structure.getNodes(), null);
    mindmapNodeRepository.saveAll(nodes);

    log.info(
        "Mindmap generated successfully with id: {} and {} nodes", mindmap.getId(), nodes.size());

    return MindmapDetailResponse.builder()
        .mindmap(mapToResponse(mindmap))
        .nodes(getNodesByMindmap(mindmap.getId()))
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public MindmapDetailResponse getMindmapById(UUID id) {
    log.info("Getting mindmap by id: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwnerOrAdmin(mindmap.getTeacherId(), getCurrentUserId());

    List<MindmapNodeResponse> nodes = getNodesByMindmap(id);

    return MindmapDetailResponse.builder()
        .mindmap(mapToResponse(mindmap))
        .nodes(nodes)
        .build();
  }

  @Override
  @Transactional
  public MindmapResponse updateMindmap(UUID id, MindmapRequest request) {
    log.info("Updating mindmap: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    if (request.getLessonId() != null) {
      lessonRepository
          .findById(request.getLessonId())
          .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
      mindmap.setLessonId(request.getLessonId());
    }

    if (request.getTitle() != null) {
      mindmap.setTitle(request.getTitle());
    }

    mindmap.setDescription(request.getDescription());
    mindmap = mindmapRepository.save(mindmap);

    log.info("Mindmap updated successfully: {}", id);
    return mapToResponse(mindmap);
  }

  @Override
  @Transactional
  public void deleteMindmap(UUID id) {
    log.info("Deleting mindmap: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    mindmap.setDeletedAt(Instant.now());
    mindmapRepository.save(mindmap);

    log.info("Mindmap deleted successfully: {}", id);
  }

  @Override
  @Transactional
  public MindmapResponse publishMindmap(UUID id) {
    log.info("Publishing mindmap: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    mindmap.setStatus(MindmapStatus.PUBLISHED);
    mindmap = mindmapRepository.save(mindmap);

    log.info("Mindmap published successfully: {}", id);
    return mapToResponse(mindmap);
  }

  @Override
  @Transactional
  public MindmapResponse archiveMindmap(UUID id) {
    log.info("Archiving mindmap: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    mindmap.setStatus(MindmapStatus.ARCHIVED);
    mindmap = mindmapRepository.save(mindmap);

    log.info("Mindmap archived successfully: {}", id);
    return mapToResponse(mindmap);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MindmapResponse> getMyMindmaps(UUID lessonId, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    log.info("Getting mindmaps for teacher: {} with lessonId: {}", currentUserId, lessonId);

    if (lessonId != null) {
      // Filter by both teacherId and lessonId
      return mindmapRepository
          .findByTeacherIdAndLessonIdAndNotDeleted(currentUserId, lessonId, pageable)
          .map(this::mapToResponse);
    } else {
      // Get all mindmaps for the teacher
      return mindmapRepository
          .findByTeacherIdAndNotDeleted(currentUserId, pageable)
          .map(this::mapToResponse);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MindmapResponse> getMindmapsByLesson(UUID lessonId, Pageable pageable) {
    log.info("Getting mindmaps for lesson: {}", lessonId);

    lessonRepository
        .findById(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mindmapRepository
        .findByLessonIdAndNotDeleted(lessonId, pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional
  public MindmapNodeResponse createNode(MindmapNodeRequest request) {
    log.info("Creating mindmap node for mindmap: {}", request.getMindmapId());

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(request.getMindmapId())
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    // Validate parent node exists if provided
    if (request.getParentId() != null) {
      mindmapNodeRepository
          .findByIdAndMindmapId(request.getParentId(), request.getMindmapId())
          .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NODE_NOT_FOUND));
    }

    MindmapNode node =
        MindmapNode.builder()
            .mindmapId(request.getMindmapId())
            .parentId(request.getParentId())
            .content(request.getContent())
            .color(request.getColor())
            .icon(request.getIcon())
            .displayOrder(
                request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .build();

    node = mindmapNodeRepository.save(node);

    log.info("Mindmap node created successfully with id: {}", node.getId());
    return mapNodeToResponse(node);
  }

  @Override
  @Transactional
  public MindmapNodeResponse updateNode(UUID nodeId, MindmapNodeRequest request) {
    log.info("Updating mindmap node: {}", nodeId);

    MindmapNode node =
        mindmapNodeRepository
            .findById(nodeId)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NODE_NOT_FOUND));

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(node.getMindmapId())
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    if (request.getContent() != null) {
      node.setContent(request.getContent());
    }
    if (request.getColor() != null) {
      node.setColor(request.getColor());
    }
    if (request.getIcon() != null) {
      node.setIcon(request.getIcon());
    }
    if (request.getDisplayOrder() != null) {
      node.setDisplayOrder(request.getDisplayOrder());
    }

    node = mindmapNodeRepository.save(node);

    log.info("Mindmap node updated successfully: {}", nodeId);
    return mapNodeToResponse(node);
  }

  @Override
  @Transactional
  public void deleteNode(UUID nodeId) {
    log.info("Deleting mindmap node: {}", nodeId);

    MindmapNode node =
        mindmapNodeRepository
            .findById(nodeId)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NODE_NOT_FOUND));

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(node.getMindmapId())
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwner(mindmap.getTeacherId(), getCurrentUserId());

    // Delete node and its children (cascade)
    mindmapNodeRepository.delete(node);

    log.info("Mindmap node deleted successfully: {}", nodeId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MindmapNodeResponse> getNodesByMindmap(UUID mindmapId) {
    log.info("Getting nodes for mindmap: {}", mindmapId);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(mindmapId)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwnerOrAdmin(mindmap.getTeacherId(), getCurrentUserId());

    List<MindmapNode> allNodes = mindmapNodeRepository.findByMindmapIdOrderByDisplayOrder(mindmapId);

    // Build hierarchical structure
    Map<UUID, MindmapNodeResponse> nodeMap = new HashMap<>();
    List<MindmapNodeResponse> rootNodes = new ArrayList<>();

    // First pass: create all node responses
    for (MindmapNode node : allNodes) {
      MindmapNodeResponse response = mapNodeToResponse(node);
      response.setChildren(new ArrayList<>());
      nodeMap.put(node.getId(), response);
    }

    // Second pass: build hierarchy
    for (MindmapNode node : allNodes) {
      MindmapNodeResponse response = nodeMap.get(node.getId());
      if (node.getParentId() == null) {
        rootNodes.add(response);
      } else {
        MindmapNodeResponse parent = nodeMap.get(node.getParentId());
        if (parent != null) {
          parent.getChildren().add(response);
        }
      }
    }

    return rootNodes;
  }

  // Helper methods

  private String buildMindmapGenerationPrompt(String userPrompt, int levels) {
    return """
        You are an expert educational content creator. Generate a mindmap structure in JSON format based on the following topic/prompt:

        %s

        IMPORTANT: The mindmap must have EXACTLY %d levels deep (including the root node).
        - Level 1: Root node
        - Level 2-%d: Child nodes at each subsequent level

        Return ONLY valid JSON in the following format (no markdown, no code blocks, no additional text):
        {
          "title": "Main topic title",
          "description": "Brief description of the mindmap",
          "nodes": [
            {
              "content": "Central/Root concept",
              "color": "#4A90E2",
              "icon": "lightbulb",
              "displayOrder": 0,
              "children": [
                {
                  "content": "Sub-topic 1",
                  "color": "#50C878",
                  "icon": "bookmark",
                  "displayOrder": 0,
                  "children": []
                },
                {
                  "content": "Sub-topic 2",
                  "color": "#FF6B6B",
                  "icon": "star",
                  "displayOrder": 1,
                  "children": []
                }
              ]
            }
          ]
        }

        Guidelines:
        - Create a hierarchical structure with 1 root node and multiple levels of children
        - The structure MUST be exactly %d levels deep
        - Use colors: #4A90E2 (blue), #50C878 (green), #FF6B6B (red), #FFD93D (yellow), #A29BFE (purple)
        - Use appropriate icons: lightbulb, bookmark, star, brain, book, target, check-circle, info-circle
        - Keep content concise (max 100 characters per node)
        - Create 3-7 main branches from root, each with 2-5 sub-nodes at each level
        - Ensure displayOrder is sequential (0, 1, 2, ...)
        """.formatted(userPrompt, levels, levels, levels);
  }

  private MindmapStructure parseMindmapFromAI(String aiResponse) throws Exception {
    // Clean the AI response - remove markdown code blocks if present
    String cleanedResponse = aiResponse.trim();
    if (cleanedResponse.startsWith("```json")) {
      cleanedResponse = cleanedResponse.substring(7);
    } else if (cleanedResponse.startsWith("```")) {
      cleanedResponse = cleanedResponse.substring(3);
    }
    if (cleanedResponse.endsWith("```")) {
      cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
    }
    cleanedResponse = cleanedResponse.trim();

    return objectMapper.readValue(cleanedResponse, MindmapStructure.class);
  }

  private List<MindmapNode> createNodesFromStructure(
      UUID mindmapId, List<NodeStructure> structures, UUID parentId) {
    List<MindmapNode> nodes = new ArrayList<>();

    for (NodeStructure structure : structures) {
      MindmapNode node =
          MindmapNode.builder()
              .mindmapId(mindmapId)
              .parentId(parentId)
              .content(structure.getContent())
              .color(structure.getColor())
              .icon(structure.getIcon())
              .displayOrder(structure.getDisplayOrder())
              .build();

      node = mindmapNodeRepository.save(node);
      nodes.add(node);

      // Recursively create children
      if (structure.getChildren() != null && !structure.getChildren().isEmpty()) {
        List<MindmapNode> children =
            createNodesFromStructure(mindmapId, structure.getChildren(), node.getId());
        nodes.addAll(children);
      }
    }

    return nodes;
  }

  private MindmapResponse mapToResponse(Mindmap mindmap) {
    String teacherName = null;
    if (mindmap.getTeacher() != null) {
      User teacher = mindmap.getTeacher();
      teacherName = teacher.getFullName() != null ? teacher.getFullName() : "Unknown";
    } else {
      teacherName =
          userRepository
              .findById(mindmap.getTeacherId())
              .map(u -> u.getFullName() != null ? u.getFullName() : "Unknown")
              .orElse("Unknown");
    }

    String lessonTitle = null;
    if (mindmap.getLesson() != null) {
      lessonTitle = mindmap.getLesson().getTitle();
    } else if (mindmap.getLessonId() != null) {
      lessonTitle =
          lessonRepository.findById(mindmap.getLessonId()).map(Lesson::getTitle).orElse(null);
    }

    long nodeCount = mindmapNodeRepository.countByMindmapId(mindmap.getId());

    return MindmapResponse.builder()
        .id(mindmap.getId())
        .teacherId(mindmap.getTeacherId())
        .teacherName(teacherName)
        .lessonId(mindmap.getLessonId())
        .lessonTitle(lessonTitle)
        .title(mindmap.getTitle())
        .description(mindmap.getDescription())
        .aiGenerated(mindmap.getAiGenerated())
        .generationPrompt(mindmap.getGenerationPrompt())
        .status(mindmap.getStatus())
        .nodeCount((int) nodeCount)
        .createdAt(mindmap.getCreatedAt())
        .updatedAt(mindmap.getUpdatedAt())
        .build();
  }

  private MindmapNodeResponse mapNodeToResponse(MindmapNode node) {
    return MindmapNodeResponse.builder()
        .id(node.getId())
        .mindmapId(node.getMindmapId())
        .parentId(node.getParentId())
        .content(node.getContent())
        .color(node.getColor())
        .icon(node.getIcon())
        .displayOrder(node.getDisplayOrder())
        .children(new ArrayList<>())
        .createdAt(node.getCreatedAt())
        .updatedAt(node.getUpdatedAt())
        .build();
  }

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private void validateTeacherRole(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    boolean isTeacher =
        user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("TEACHER") || role.getName().equals("ADMIN"));

    if (!isTeacher) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }

  private void validateOwner(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId)) {
      throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
    }
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (ownerId.equals(currentUserId)) {
      return;
    }

    User user =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));

    if (!isAdmin) {
      throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
    }
  }

  // Inner classes for AI response parsing
  @lombok.Data
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  private static class MindmapStructure {
    private String title;
    private String description;
    private List<NodeStructure> nodes;
  }

  @lombok.Data
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  private static class NodeStructure {
    private String content;
    private String color;
    private String icon;
    private Integer displayOrder;
    private List<NodeStructure> children;
  }
}
