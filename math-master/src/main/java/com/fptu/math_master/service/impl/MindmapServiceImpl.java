package com.fptu.math_master.service.impl;

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
import com.fptu.math_master.service.TokenCostConfigService;
import com.fptu.math_master.service.UserSubscriptionService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

  private static final String EXPORT_FORMAT_PDF = "pdf";
  private static final String EXPORT_FORMAT_PNG = "png";

  MindmapRepository mindmapRepository;
  MindmapNodeRepository mindmapNodeRepository;
  UserRepository userRepository;
  LessonRepository lessonRepository;
  GeminiService geminiService;
  UserSubscriptionService userSubscriptionService;
  TokenCostConfigService tokenCostConfigService;
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
    int cost = tokenCostConfigService.getCostPerUse("mindmap");
    userSubscriptionService.consumeMyTokens(cost, "MINDMAP");

    // Build the AI prompt with levels
    Integer levels = request.getLevels() != null ? request.getLevels() : 3;
    String aiPrompt = buildMindmapGenerationPrompt(request.getPrompt(), levels);

    // Call Gemini AI to generate mindmap structure
    log.info("[Mindmap AI Prompt]\n{}", aiPrompt);
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

    // Create nodes from the structure with level-based colors
    List<MindmapNode> nodes =
        createNodesFromStructure(mindmap.getId(), structure.getNodes(), null, 1);
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
    long startedAt = System.currentTimeMillis();

    // Use optimized query with JOIN FETCH to load teacher and lesson in one query
    Mindmap mindmap =
        mindmapRepository
            .findByIdWithDetailsAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));
    long loadedMindmapAt = System.currentTimeMillis();

    validateOwnerOrAdmin(mindmap.getTeacherId(), getCurrentUserId());
    long validatedPermissionAt = System.currentTimeMillis();

    // Fetch all nodes in one query
    List<MindmapNode> allNodes = getSortedNodesByMindmapId(mindmap.getId());
    long loadedNodesAt = System.currentTimeMillis();

    // Build hierarchical structure
    List<MindmapNodeResponse> nodes = buildNodeHierarchy(allNodes);
    long builtHierarchyAt = System.currentTimeMillis();

    // Pass node count directly instead of querying again
    MindmapResponse mindmapResponse = mapToResponseWithNodeCount(mindmap, allNodes.size());

    log.info(
        "Mindmap detail timing id={} total={}ms, loadMindmap={}ms, auth={}ms, loadNodes={}ms, buildTree={}ms, nodeCount={}",
        id,
        builtHierarchyAt - startedAt,
        loadedMindmapAt - startedAt,
        validatedPermissionAt - loadedMindmapAt,
        loadedNodesAt - validatedPermissionAt,
        builtHierarchyAt - loadedNodesAt,
        allNodes.size());

    return MindmapDetailResponse.builder().mindmap(mindmapResponse).nodes(nodes).build();
  }

  @Override
  @Transactional(readOnly = true)
  public MindmapDetailResponse getPublicMindmapById(UUID id) {
    log.info("Getting public mindmap by id: {}", id);

    Mindmap mindmap =
        mindmapRepository
            .findByIdWithDetailsAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    if (mindmap.getStatus() != MindmapStatus.PUBLISHED) {
      throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
    }

    List<MindmapNode> allNodes = getSortedNodesByMindmapId(mindmap.getId());
    List<MindmapNodeResponse> nodes = buildNodeHierarchy(allNodes);
    MindmapResponse mindmapResponse = mapToResponseWithNodeCount(mindmap, allNodes.size());

    return MindmapDetailResponse.builder().mindmap(mindmapResponse).nodes(nodes).build();
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
  public Page<MindmapResponse> getMyMindmaps(
      UUID gradeId, UUID subjectId, UUID chapterId, UUID lessonId, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    log.info(
        "Getting mindmaps for teacher: {} with filters gradeId={}, subjectId={}, chapterId={}, lessonId={}",
        currentUserId,
        gradeId,
        subjectId,
        chapterId,
        lessonId);
    long startedAt = System.currentTimeMillis();

    Page<Mindmap> mindmaps =
        mindmapRepository.findByTeacherWithHierarchyFilters(
            currentUserId, gradeId, subjectId, chapterId, lessonId, pageable);

    List<Mindmap> content = mindmaps.getContent();
    long loadedMindmapsAt = System.currentTimeMillis();

    // Batch fetch node counts to avoid N+1
    List<UUID> mindmapIds = content.stream().map(Mindmap::getId).toList();
    Map<UUID, Long> nodeCounts = getNodeCountsForMindmaps(mindmapIds);
    long loadedNodeCountsAt = System.currentTimeMillis();

    // Batch resolve names for entities that were not join-fetched
    Map<UUID, String> teacherNames = getTeacherNamesForMindmaps(content);
    Map<UUID, String> lessonTitles = getLessonTitlesForMindmaps(content);
    long loadedNamesAt = System.currentTimeMillis();

    log.info(
        "My mindmaps timing teacherId={} total={}ms, loadPage={}ms, nodeCounts={}ms, names={}ms, pageSize={}",
        currentUserId,
        loadedNamesAt - startedAt,
        loadedMindmapsAt - startedAt,
        loadedNodeCountsAt - loadedMindmapsAt,
        loadedNamesAt - loadedNodeCountsAt,
        content.size());

    return mindmaps.map(
        m ->
            mapToResponseWithNodeCount(
                m, nodeCounts.getOrDefault(m.getId(), 0L).intValue(), teacherNames, lessonTitles));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MindmapResponse> getMindmapsByLesson(UUID lessonId, Pageable pageable) {
    log.info("Getting mindmaps for lesson: {}", lessonId);
    long startedAt = System.currentTimeMillis();

    lessonRepository
        .findById(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    Page<Mindmap> mindmaps;
    if (isStudent(currentUserId)) {
      mindmaps =
          mindmapRepository.findByLessonIdAndStatusWithDetailsAndNotDeleted(
              lessonId, MindmapStatus.PUBLISHED, pageable);
    } else {
      mindmaps = mindmapRepository.findByLessonIdWithDetailsAndNotDeleted(lessonId, pageable);
    }

    List<Mindmap> content = mindmaps.getContent();
    long loadedMindmapsAt = System.currentTimeMillis();

    // Batch fetch node counts to avoid N+1
    List<UUID> mindmapIds = content.stream().map(Mindmap::getId).toList();
    Map<UUID, Long> nodeCounts = getNodeCountsForMindmaps(mindmapIds);
    long loadedNodeCountsAt = System.currentTimeMillis();

    // Batch resolve names for entities that were not join-fetched
    Map<UUID, String> teacherNames = getTeacherNamesForMindmaps(content);
    Map<UUID, String> lessonTitles = getLessonTitlesForMindmaps(content);
    long loadedNamesAt = System.currentTimeMillis();

    log.info(
        "Lesson mindmaps timing lessonId={} total={}ms, loadPage={}ms, nodeCounts={}ms, names={}ms, pageSize={}",
        lessonId,
        loadedNamesAt - startedAt,
        loadedMindmapsAt - startedAt,
        loadedNodeCountsAt - loadedMindmapsAt,
        loadedNamesAt - loadedNodeCountsAt,
        content.size());

    return mindmaps.map(
        m ->
            mapToResponseWithNodeCount(
                m, nodeCounts.getOrDefault(m.getId(), 0L).intValue(), teacherNames, lessonTitles));
  }

    @Override
    @Transactional(readOnly = true)
    public Page<MindmapResponse> getPublicMindmaps(
      UUID gradeId,
      UUID subjectId,
      UUID chapterId,
      UUID lessonId,
      String name,
      Pageable pageable) {
      String normalizedName = name == null ? null : name.trim();
      Page<Mindmap> mindmaps =
          mindmapRepository.findPublicWithFilters(
          MindmapStatus.PUBLISHED,
          gradeId,
          subjectId,
          chapterId,
          lessonId,
          normalizedName,
          pageable);

    List<Mindmap> content = mindmaps.getContent();
    List<UUID> mindmapIds = content.stream().map(Mindmap::getId).toList();
    Map<UUID, Long> nodeCounts = getNodeCountsForMindmaps(mindmapIds);
    Map<UUID, String> teacherNames = getTeacherNamesForMindmaps(content);
    Map<UUID, String> lessonTitles = getLessonTitlesForMindmaps(content);

    return mindmaps.map(
      m ->
        mapToResponseWithNodeCount(
          m, nodeCounts.getOrDefault(m.getId(), 0L).intValue(), teacherNames, lessonTitles));
    }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData exportMindmap(UUID id, String format) {
    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    validateOwnerOrAdmin(mindmap.getTeacherId(), getCurrentUserId());
    return exportMindmapFile(mindmap, normalizeExportFormat(format));
  }

  @Override
  @Transactional(readOnly = true)
  public BinaryFileData exportPublicMindmap(UUID id, String format) {
    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    if (mindmap.getStatus() != MindmapStatus.PUBLISHED) {
      throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
    }

    return exportMindmapFile(mindmap, normalizeExportFormat(format));
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
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
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

    UUID currentUserId = getCurrentUserId();
    if (isStudent(currentUserId)) {
      if (mindmap.getStatus() != MindmapStatus.PUBLISHED) {
        throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
      }
    } else {
      validateOwnerOrAdmin(mindmap.getTeacherId(), currentUserId);
    }

    List<MindmapNode> allNodes = getSortedNodesByMindmapId(mindmapId);
    return buildNodeHierarchy(allNodes);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MindmapNodeResponse> getPublicNodesByMindmap(UUID mindmapId) {
    log.info("Getting public nodes for mindmap: {}", mindmapId);

    Mindmap mindmap =
        mindmapRepository
            .findByIdAndNotDeleted(mindmapId)
            .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

    if (mindmap.getStatus() != MindmapStatus.PUBLISHED) {
      throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
    }

    List<MindmapNode> allNodes = getSortedNodesByMindmapId(mindmapId);
    return buildNodeHierarchy(allNodes);
  }

  private List<MindmapNode> getSortedNodesByMindmapId(UUID mindmapId) {
    List<MindmapNode> nodes = mindmapNodeRepository.findByMindmapId(mindmapId);
    nodes.sort(
        Comparator.comparing(
            MindmapNode::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
    return nodes;
  }

  // Helper methods

  private String buildMindmapGenerationPrompt(String userPrompt, int levels) {
    return """
        You are an expert educational content creator. Generate a mindmap structure in JSON format based on the following topic/prompt:

        %s

        IMPORTANT: The mindmap must have EXACTLY %d levels deep (including the root node).
        - Level 1: Root node (will use blue color)
        - Level 2: Child nodes (will use green color)
        - Level 3+: Subsequent levels (will use red, yellow, purple colors)

        CRITICAL: All nodes at the SAME LEVEL must have the SAME COLOR.
        - Level 1 (root): #4A90E2 (blue)
        - Level 2 (main branches): #50C878 (green)
        - Level 3: #FF6B6B (red)
        - Level 4: #FFD93D (yellow)
        - Level 5: #A29BFE (purple)
        - Level 6+: #FF8C94 (pink)

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
                  "color": "#50C878",
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
        - IMPORTANT: All nodes at the same level MUST have the same color
        - Use appropriate icons: lightbulb, bookmark, star, brain, book, target, check-circle, info-circle
        - Keep content concise (max 100 characters per node)
        - Create 3-7 main branches from root, each with 2-5 sub-nodes at each level
        - Ensure displayOrder is sequential (0, 1, 2, ...)
        - LANGUAGE: All node content ("content" field), "title", and "description" MUST be written in Vietnamese with full diacritics (tiếng Việt có dấu). Do NOT use English or unaccented Vietnamese.
        """
        .formatted(userPrompt, levels, levels);
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

  /**
   * Color palette for each level to ensure consistent coloring
   */
  private static final String[] LEVEL_COLORS = {
    "#4A90E2", // Level 1 - Blue (root)
    "#50C878", // Level 2 - Green (main branches)
    "#FF6B6B", // Level 3 - Red
    "#FFD93D", // Level 4 - Yellow
    "#A29BFE", // Level 5 - Purple
    "#FF8C94" // Level 6+ - Pink
  };

  private List<MindmapNode> createNodesFromStructure(
      UUID mindmapId, List<NodeStructure> structures, UUID parentId, int level) {
    List<MindmapNode> nodes = new ArrayList<>();

    // Get color for this level (use last color if exceeds array length)
    String levelColor =
        level <= LEVEL_COLORS.length
            ? LEVEL_COLORS[level - 1]
            : LEVEL_COLORS[LEVEL_COLORS.length - 1];

    for (NodeStructure structure : structures) {
      MindmapNode node =
          MindmapNode.builder()
              .mindmapId(mindmapId)
              .parentId(parentId)
              .content(structure.getContent())
              .color(levelColor) // Override with level-based color
              .icon(structure.getIcon())
              .displayOrder(structure.getDisplayOrder())
              .build();

      node = mindmapNodeRepository.save(node);
      nodes.add(node);

      // Recursively create children with incremented level
      if (structure.getChildren() != null && !structure.getChildren().isEmpty()) {
        List<MindmapNode> children =
            createNodesFromStructure(mindmapId, structure.getChildren(), node.getId(), level + 1);
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

  /**
   * Optimized version that accepts node count to avoid extra query
   */
  private MindmapResponse mapToResponseWithNodeCount(Mindmap mindmap, int nodeCount) {
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
        .nodeCount(nodeCount)
        .createdAt(mindmap.getCreatedAt())
        .updatedAt(mindmap.getUpdatedAt())
        .build();
  }

  /**
   * Optimized version for paged endpoints that resolves teacher/lesson names in batch.
   */
  private MindmapResponse mapToResponseWithNodeCount(
      Mindmap mindmap,
      int nodeCount,
      Map<UUID, String> teacherNames,
      Map<UUID, String> lessonTitles) {
    String teacherName =
        Optional.ofNullable(teacherNames.get(mindmap.getTeacherId())).orElse("Unknown");
    String lessonTitle =
        mindmap.getLessonId() == null ? null : lessonTitles.get(mindmap.getLessonId());

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
        .nodeCount(nodeCount)
        .createdAt(mindmap.getCreatedAt())
        .updatedAt(mindmap.getUpdatedAt())
        .build();
  }

  /**
   * Build node hierarchy from flat list without additional queries
   */
  private List<MindmapNodeResponse> buildNodeHierarchy(List<MindmapNode> allNodes) {
    Map<UUID, MindmapNodeResponse> nodeMap = new HashMap<>();
    Map<UUID, UUID> parentMap = new HashMap<>();
    List<MindmapNodeResponse> rootNodes = new ArrayList<>();

    // First pass: create all node responses
    for (MindmapNode node : allNodes) {
      MindmapNodeResponse response = mapNodeToResponse(node);
      response.setChildren(new ArrayList<>());
      nodeMap.put(node.getId(), response);
      parentMap.put(node.getId(), node.getParentId());
    }

    Set<UUID> cycleNodes = findCycleNodes(parentMap);

    // Second pass: build hierarchy
    for (MindmapNode node : allNodes) {
      MindmapNodeResponse response = nodeMap.get(node.getId());
      if (node.getParentId() == null) {
        rootNodes.add(response);
      } else {
        // Guard against corrupted data that introduces parent cycles.
        if (cycleNodes.contains(node.getId())) {
          log.warn(
              "Detected cyclic hierarchy in mindmap node graph. nodeId={}, parentId={}. Treating as root.",
              node.getId(),
              node.getParentId());
          rootNodes.add(response);
          continue;
        }

        MindmapNodeResponse parent = nodeMap.get(node.getParentId());
        if (parent != null) {
          parent.getChildren().add(response);
        } else {
          // Parent may have been deleted; keep node visible as a root node.
          rootNodes.add(response);
        }
      }
    }

    return rootNodes;
  }

  private BinaryFileData exportMindmapFile(Mindmap mindmap, String format) {
    List<MindmapNode> allNodes = getSortedNodesByMindmapId(mindmap.getId());
    List<ExportNodeLine> lines = buildExportLines(allNodes);
    BufferedImage image = renderMindmapImage(mindmap.getTitle(), lines);

    String baseName = sanitizeFileName(mindmap.getTitle() == null ? "mindmap" : mindmap.getTitle());
    if (EXPORT_FORMAT_PNG.equals(format)) {
      return new BinaryFileData(toPngBytes(image), baseName + ".png", "image/png");
    }

    return new BinaryFileData(toPdfBytes(image), baseName + ".pdf", "application/pdf");
  }

  private String normalizeExportFormat(String format) {
    if (format == null || format.isBlank()) {
      return EXPORT_FORMAT_PDF;
    }
    String normalized = format.trim().toLowerCase(Locale.ROOT);
    if (!EXPORT_FORMAT_PDF.equals(normalized) && !EXPORT_FORMAT_PNG.equals(normalized)) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }
    return normalized;
  }

  private List<ExportNodeLine> buildExportLines(List<MindmapNode> allNodes) {
    Map<UUID, List<MindmapNode>> childrenMap = new HashMap<>();
    List<MindmapNode> roots = new ArrayList<>();

    Set<UUID> existingNodeIds = allNodes.stream().map(MindmapNode::getId).collect(Collectors.toSet());

    for (MindmapNode node : allNodes) {
      UUID parentId = node.getParentId();
      if (parentId == null || !existingNodeIds.contains(parentId)) {
        roots.add(node);
      } else {
        childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
      }
    }

    roots.sort(Comparator.comparing(MindmapNode::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
    childrenMap.values().forEach(
        list -> list.sort(Comparator.comparing(MindmapNode::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))));

    List<ExportNodeLine> lines = new ArrayList<>();
    for (MindmapNode root : roots) {
      appendExportLines(root, 0, childrenMap, lines);
    }
    return lines;
  }

  private void appendExportLines(
      MindmapNode node,
      int depth,
      Map<UUID, List<MindmapNode>> childrenMap,
      List<ExportNodeLine> lines) {
    lines.add(new ExportNodeLine(depth, safeText(node.getContent()), node.getColor()));
    for (MindmapNode child : childrenMap.getOrDefault(node.getId(), Collections.emptyList())) {
      appendExportLines(child, depth + 1, childrenMap, lines);
    }
  }

  private BufferedImage renderMindmapImage(String title, List<ExportNodeLine> lines) {
    final int leftPadding = 40;
    final int topPadding = 40;
    final int headerHeight = 52;
    final int rowHeight = 42;
    final int indentWidth = 54;
    final int contentMaxChars = 90;

    int maxDepth = lines.stream().mapToInt(ExportNodeLine::depth).max().orElse(0);
    int width = Math.max(1280, 900 + maxDepth * indentWidth);
    int height = Math.max(720, topPadding + headerHeight + Math.max(1, lines.size()) * rowHeight + 40);

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, height);

      g.setColor(new Color(23, 43, 77));
      g.setFont(new Font("SansSerif", Font.BOLD, 28));
      g.drawString(safeText(title), leftPadding, topPadding);

      int y = topPadding + headerHeight;
      for (ExportNodeLine line : lines) {
        int x = leftPadding + line.depth() * indentWidth;

        Color nodeColor = parseColor(line.color());
        g.setColor(nodeColor);
        g.fillRoundRect(x, y - 24, 18, 18, 6, 6);

        g.setColor(new Color(220, 228, 240));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x + 24, y - 28, Math.max(220, width - x - 80), 28, 8, 8);

        g.setColor(new Color(34, 41, 57));
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String content = line.content();
        if (content.length() > contentMaxChars) {
          content = content.substring(0, contentMaxChars - 3) + "...";
        }
        g.drawString(content, x + 32, y - 8);

        y += rowHeight;
      }
    } finally {
      g.dispose();
    }
    return image;
  }

  private byte[] toPngBytes(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (Exception ex) {
      log.error("Failed to export mindmap PNG", ex);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  private byte[] toPdfBytes(BufferedImage image) {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      float width = image.getWidth();
      float height = image.getHeight();
      PDPage page = new PDPage(new PDRectangle(width, height));
      document.addPage(page);

      PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        contentStream.drawImage(pdImage, 0, 0, width, height);
      }

      document.save(out);
      return out.toByteArray();
    } catch (Exception ex) {
      log.error("Failed to export mindmap PDF", ex);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  private Color parseColor(String colorHex) {
    if (colorHex == null || colorHex.isBlank()) {
      return new Color(76, 110, 245);
    }
    try {
      return Color.decode(colorHex.trim());
    } catch (Exception ex) {
      return new Color(76, 110, 245);
    }
  }

  private String sanitizeFileName(String raw) {
    return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private String safeText(String text) {
    if (text == null || text.isBlank()) {
      return "(empty)";
    }
    return text.replaceAll("\\s+", " ").trim();
  }

  /**
   * Detect all nodes that belong to parent-pointer cycles in O(n).
   */
  private Set<UUID> findCycleNodes(Map<UUID, UUID> parentMap) {
    Set<UUID> cycleNodes = new HashSet<>();
    Map<UUID, Integer> state = new HashMap<>();

    for (UUID nodeId : parentMap.keySet()) {
      if (state.getOrDefault(nodeId, 0) != 0) {
        continue;
      }

      List<UUID> path = new ArrayList<>();
      Map<UUID, Integer> indexInPath = new HashMap<>();
      UUID cursor = nodeId;

      while (cursor != null && parentMap.containsKey(cursor)) {
        Integer nodeState = state.getOrDefault(cursor, 0);
        if (nodeState == 2) {
          break;
        }
        if (nodeState == 1) {
          Integer cycleStart = indexInPath.get(cursor);
          if (cycleStart != null) {
            for (int i = cycleStart; i < path.size(); i++) {
              cycleNodes.add(path.get(i));
            }
          }
          break;
        }

        state.put(cursor, 1);
        indexInPath.put(cursor, path.size());
        path.add(cursor);
        cursor = parentMap.get(cursor);
      }

      for (UUID visitedNode : path) {
        state.put(visitedNode, 2);
      }
    }

    return cycleNodes;
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
    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
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

  private boolean isStudent(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return user.getRoles().stream().anyMatch(role -> role.getName().equals("STUDENT"));
  }

  /**
   * Batch fetch node counts for multiple mindmaps in one query to avoid N+1
   */
  private Map<UUID, Long> getNodeCountsForMindmaps(List<UUID> mindmapIds) {
    if (mindmapIds == null || mindmapIds.isEmpty()) {
      return new HashMap<>();
    }

    return mindmapNodeRepository.countByMindmapIds(mindmapIds).stream()
        .collect(
            Collectors.toMap(
                MindmapNodeRepository.NodeCountProjection::getMindmapId,
                MindmapNodeRepository.NodeCountProjection::getCount));
  }

  private Map<UUID, String> getTeacherNamesForMindmaps(List<Mindmap> mindmaps) {
    if (mindmaps == null || mindmaps.isEmpty()) {
      return new HashMap<>();
    }

    Map<UUID, String> result = new HashMap<>();
    Set<UUID> missingTeacherIds = new HashSet<>();

    for (Mindmap mindmap : mindmaps) {
      User teacher = mindmap.getTeacher();
      if (teacher != null) {
        result.put(
            mindmap.getTeacherId(),
            teacher.getFullName() != null ? teacher.getFullName() : "Unknown");
      } else {
        missingTeacherIds.add(mindmap.getTeacherId());
      }
    }

    if (!missingTeacherIds.isEmpty()) {
      userRepository
          .findAllById(missingTeacherIds)
          .forEach(
              user ->
                  result.put(
                      user.getId(), user.getFullName() != null ? user.getFullName() : "Unknown"));
    }

    return result;
  }

  private Map<UUID, String> getLessonTitlesForMindmaps(List<Mindmap> mindmaps) {
    if (mindmaps == null || mindmaps.isEmpty()) {
      return new HashMap<>();
    }

    Map<UUID, String> result = new HashMap<>();
    Set<UUID> missingLessonIds = new HashSet<>();

    for (Mindmap mindmap : mindmaps) {
      UUID lessonId = mindmap.getLessonId();
      if (lessonId == null) {
        continue;
      }

      Lesson lesson = mindmap.getLesson();
      if (lesson != null) {
        result.put(lessonId, lesson.getTitle());
      } else {
        missingLessonIds.add(lessonId);
      }
    }

    if (!missingLessonIds.isEmpty()) {
      lessonRepository
          .findAllById(missingLessonIds)
          .forEach(lesson -> result.put(lesson.getId(), lesson.getTitle()));
    }

    return result;
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

  private record ExportNodeLine(int depth, String content, String color) {}
}
