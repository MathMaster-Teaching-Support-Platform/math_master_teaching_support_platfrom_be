package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.GenerateMindmapRequest;
import com.fptu.math_master.dto.request.MindmapNodeRequest;
import com.fptu.math_master.dto.request.MindmapRequest;
import com.fptu.math_master.dto.response.MindmapResponse;
import com.fptu.math_master.entity.Mindmap;
import com.fptu.math_master.entity.MindmapNode;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.MindmapStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.MindmapNodeRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.UserSubscriptionService;
import java.awt.Color;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("MindmapServiceImpl - Tests")
class MindmapServiceImplTest extends BaseUnitTest {

  @InjectMocks private MindmapServiceImpl mindmapService;

  @Mock private MindmapRepository mindmapRepository;
  @Mock private MindmapNodeRepository mindmapNodeRepository;
  @Mock private UserRepository userRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private GeminiService geminiService;
  @Mock private UserSubscriptionService userSubscriptionService;
  @Mock private ObjectMapper objectMapper;

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = MindmapServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(mindmapService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  private void mockJwtAuth(UUID userId) {
    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", userId.toString(), "scope", "ROLE_TEACHER"));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  private User buildTeacher(UUID id) {
    Role role = new Role();
    role.setName("TEACHER");
    User user = new User();
    user.setId(id);
    user.setFullName("Nguyen Van A");
    user.setRoles(Set.of(role));
    return user;
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("public mindmap operations")
  class PublicMindmapOperationTests {

    @Test
    void it_should_create_mindmap_when_current_user_has_teacher_role() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("30000000-0000-0000-0000-000000000001");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(mindmapRepository.save(any(Mindmap.class)))
          .thenAnswer(
              invocation -> {
                Mindmap m = invocation.getArgument(0);
                m.setId(UUID.fromString("30000000-0000-0000-0000-000000000002"));
                return m;
              });

      MindmapRequest request = new MindmapRequest();
      request.setTitle("Hàm số bậc hai");
      request.setDescription("Tổng quan chương hàm số");

      // ===== ACT =====
      var response = mindmapService.createMindmap(request);

      // ===== ASSERT =====
      assertNotNull(response);
      assertEquals("Hàm số bậc hai", response.getTitle());

      // ===== VERIFY =====
      verify(mindmapRepository, times(1)).save(any(Mindmap.class));
    }

    @Test
    void it_should_publish_mindmap_when_owner_matches_current_user() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("40000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("40000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setStatus(MindmapStatus.DRAFT);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(mindmapRepository.save(any(Mindmap.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      var response = mindmapService.publishMindmap(mindmapId);

      // ===== ASSERT =====
      assertEquals(MindmapStatus.PUBLISHED, response.getStatus());

      // ===== VERIFY =====
      verify(mindmapRepository, times(1)).save(mindmap);
    }

    @Test
    void it_should_throw_generation_failed_when_gemini_service_throws_exception() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("50000000-0000-0000-0000-000000000001");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(geminiService.sendMessage(any())).thenThrow(new RuntimeException("AI is down"));

      GenerateMindmapRequest request =
          GenerateMindmapRequest.builder()
              .prompt("Hệ thức lượng trong tam giác vuông")
              .title("Mindmap lượng giác")
              .levels(3)
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> mindmapService.generateMindmap(request));
      assertEquals(ErrorCode.MINDMAP_GENERATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("node and export flows")
  class NodeAndExportFlowTests {

    @Test
    void it_should_throw_access_denied_when_student_reads_nodes_of_unpublished_mindmap() {
      // ===== ARRANGE =====
      UUID studentId = UUID.fromString("60000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("60000000-0000-0000-0000-000000000002");
      Role studentRole = new Role();
      studentRole.setName("STUDENT");
      User student = new User();
      student.setId(studentId);
      student.setRoles(Set.of(studentRole));
      mockJwtAuth(studentId);

      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(UUID.fromString("60000000-0000-0000-0000-000000000003"));
      mindmap.setStatus(MindmapStatus.DRAFT);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> mindmapService.getNodesByMindmap(mindmapId));
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_export_mindmap_as_png_when_owner_requests_png_format() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("70000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("70000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);

      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setTitle("Mindmap hàm số");
      mindmap.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(mindmapNodeRepository.findByMindmapId(mindmapId))
          .thenReturn(new java.util.ArrayList<>());

      // ===== ACT =====
      var data = mindmapService.exportMindmap(mindmapId, "png");

      // ===== ASSERT =====
      assertNotNull(data);
      assertEquals("image/png", data.contentType());
      assertTrue(data.content().length > 0);
    }

    @Test
    void it_should_reject_public_export_when_mindmap_is_not_published() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("80000000-0000-0000-0000-000000000001");
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
      mindmap.setStatus(MindmapStatus.DRAFT);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> mindmapService.exportPublicMindmap(mindmapId, "pdf"));
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_create_node_when_parent_is_null_and_owner_matches() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("81000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("81000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(mindmapNodeRepository.save(any(MindmapNode.class)))
          .thenAnswer(
              invocation -> {
                MindmapNode n = invocation.getArgument(0);
                n.setId(UUID.fromString("81000000-0000-0000-0000-000000000003"));
                return n;
              });
      MindmapNodeRequest request =
          MindmapNodeRequest.builder()
              .mindmapId(mindmapId)
              .content("Nút gốc")
              .color("#4A90E2")
              .displayOrder(0)
              .build();

      // ===== ACT =====
      var response = mindmapService.createNode(request);

      // ===== ASSERT =====
      assertEquals("Nút gốc", response.getContent());
      assertEquals(mindmapId, response.getMindmapId());
    }

    @Test
    void it_should_archive_mindmap_when_owner_matches() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("82000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("82000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(mindmapRepository.save(any(Mindmap.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      var response = mindmapService.archiveMindmap(mindmapId);

      // ===== ASSERT =====
      assertEquals(MindmapStatus.ARCHIVED, response.getStatus());
    }
  }

  @Nested
  @DisplayName("paged query flows")
  class PagedQueryFlowTests {

    @Test
    void it_should_get_my_mindmaps_when_teacher_requests_without_lesson_filter() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a1000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setTitle("Hàm số");
      when(mindmapRepository.findByTeacherIdWithDetailsAndNotDeleted(teacherId, PageRequest.of(0, 5)))
          .thenReturn(new PageImpl<>(List.of(mindmap), PageRequest.of(0, 5), 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of());
      when(userRepository.findAllById(Set.of(teacherId))).thenReturn(List.of(buildTeacher(teacherId)));

      // ===== ACT =====
      var page = mindmapService.getMyMindmaps(null, PageRequest.of(0, 5));

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());
      assertEquals("Hàm số", page.getContent().get(0).getTitle());
    }

    @Test
    void it_should_return_public_mindmaps_with_trimmed_name_filter() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("a2000000-0000-0000-0000-000000000001");
      UUID teacherId = UUID.fromString("a2000000-0000-0000-0000-000000000002");
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setTitle("Lượng giác");
      when(mindmapRepository.findPublicWithFilters(
              MindmapStatus.PUBLISHED, null, "Lượng giác", PageRequest.of(0, 5)))
          .thenReturn(new PageImpl<>(List.of(mindmap), PageRequest.of(0, 5), 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of());
      when(userRepository.findAllById(Set.of(teacherId))).thenReturn(List.of(buildTeacher(teacherId)));

      // ===== ACT =====
      var page = mindmapService.getPublicMindmaps(null, "  Lượng giác  ", PageRequest.of(0, 5));

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());
      assertEquals("Lượng giác", page.getContent().get(0).getTitle());
    }

    @Test
    void it_should_get_mindmaps_by_lesson_for_student_using_published_filter() {
      // ===== ARRANGE =====
      UUID studentId = UUID.fromString("a2100000-0000-0000-0000-000000000001");
      UUID lessonId = UUID.fromString("a2100000-0000-0000-0000-000000000002");
      mockJwtAuth(studentId);
      Role studentRole = new Role();
      studentRole.setName("STUDENT");
      User student = new User();
      student.setId(studentId);
      student.setRoles(Set.of(studentRole));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
      com.fptu.math_master.entity.Lesson lesson = new com.fptu.math_master.entity.Lesson();
      lesson.setTitle("Bài học");
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
      Mindmap mindmap = new Mindmap();
      mindmap.setId(UUID.fromString("a2100000-0000-0000-0000-000000000003"));
      mindmap.setTeacherId(UUID.fromString("a2100000-0000-0000-0000-000000000004"));
      when(mindmapRepository.findByLessonIdAndStatusWithDetailsAndNotDeleted(
              lessonId, MindmapStatus.PUBLISHED, PageRequest.of(0, 5)))
          .thenReturn(new PageImpl<>(List.of(mindmap), PageRequest.of(0, 5), 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmap.getId()))).thenReturn(List.of());
      when(userRepository.findAllById(Set.of(mindmap.getTeacherId())))
          .thenReturn(List.of(buildTeacher(mindmap.getTeacherId())));

      // ===== ACT =====
      var page = mindmapService.getMindmapsByLesson(lessonId, PageRequest.of(0, 5));

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());
    }
  }

  @Nested
  @DisplayName("mapping helpers")
  class MappingHelperTests {

    @Test
    void it_should_map_to_response_with_teacher_entity_without_repository_lookup() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a3000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a3000000-0000-0000-0000-000000000002");
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      mindmap.setTitle("Số phức");
      User teacher = buildTeacher(teacherId);
      mindmap.setTeacher(teacher);
      when(mindmapNodeRepository.countByMindmapId(mindmapId)).thenReturn(3L);

      // ===== ACT =====
      MindmapResponse response =
          invokePrivate("mapToResponse", new Class<?>[] {Mindmap.class}, mindmap);

      // ===== ASSERT =====
      assertEquals("Nguyen Van A", response.getTeacherName());
      assertEquals(3, response.getNodeCount());
    }

    @Test
    void it_should_build_hierarchy_and_treat_missing_parent_as_root() {
      // ===== ARRANGE =====
      UUID rootId = UUID.fromString("a4000000-0000-0000-0000-000000000001");
      UUID orphanId = UUID.fromString("a4000000-0000-0000-0000-000000000002");
      UUID missingParent = UUID.fromString("a4000000-0000-0000-0000-000000000999");
      MindmapNode root = new MindmapNode();
      root.setId(rootId);
      root.setMindmapId(UUID.randomUUID());
      root.setParentId(null);
      root.setContent("Root");
      root.setDisplayOrder(0);
      MindmapNode orphan = new MindmapNode();
      orphan.setId(orphanId);
      orphan.setMindmapId(root.getMindmapId());
      orphan.setParentId(missingParent);
      orphan.setContent("Orphan");
      orphan.setDisplayOrder(1);

      // ===== ACT =====
      List<?> roots =
          invokePrivate("buildNodeHierarchy", new Class<?>[] {List.class}, List.of(root, orphan));

      // ===== ASSERT =====
      assertEquals(2, roots.size());
    }
  }

  @Nested
  @DisplayName("additional public operations")
  class AdditionalPublicOperationTests {

    @Test
    void it_should_throw_not_found_when_updating_non_existing_mindmap() {
      // ===== ARRANGE =====
      UUID id = UUID.fromString("a5000000-0000-0000-0000-000000000001");
      when(mindmapRepository.findByIdAndNotDeleted(id)).thenReturn(Optional.empty());
      MindmapRequest request = new MindmapRequest();
      request.setTitle("Update title");

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> mindmapService.updateMindmap(id, request));
      assertEquals(ErrorCode.MINDMAP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_soft_delete_mindmap_when_owner_deletes() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a5100000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a5100000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      when(mindmapRepository.save(any(Mindmap.class))).thenAnswer(i -> i.getArgument(0));

      // ===== ACT =====
      mindmapService.deleteMindmap(mindmapId);

      // ===== ASSERT =====
      assertNotNull(m.getDeletedAt());
    }

    @Test
    void it_should_return_public_mindmap_detail_when_status_is_published() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("a5200000-0000-0000-0000-000000000001");
      UUID teacherId = UUID.fromString("a5200000-0000-0000-0000-000000000002");
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      m.setTitle("Public map");
      m.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdWithDetailsAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      MindmapNode node = new MindmapNode();
      node.setId(UUID.fromString("a5200000-0000-0000-0000-000000000003"));
      node.setMindmapId(mindmapId);
      node.setParentId(null);
      node.setContent("Root");
      node.setDisplayOrder(0);
      when(mindmapNodeRepository.findByMindmapId(mindmapId))
          .thenReturn(new java.util.ArrayList<>(List.of(node)));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));

      // ===== ACT =====
      var detail = mindmapService.getPublicMindmapById(mindmapId);

      // ===== ASSERT =====
      assertEquals("Public map", detail.getMindmap().getTitle());
      assertEquals(1, detail.getNodes().size());
    }

    @Test
    void it_should_return_public_nodes_when_mindmap_is_published() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("a5300000-0000-0000-0000-000000000001");
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      MindmapNode node = new MindmapNode();
      node.setId(UUID.fromString("a5300000-0000-0000-0000-000000000002"));
      node.setMindmapId(mindmapId);
      node.setParentId(null);
      node.setContent("Public root");
      node.setDisplayOrder(0);
      when(mindmapNodeRepository.findByMindmapId(mindmapId))
          .thenReturn(new java.util.ArrayList<>(List.of(node)));

      // ===== ACT =====
      var nodes = mindmapService.getPublicNodesByMindmap(mindmapId);

      // ===== ASSERT =====
      assertEquals(1, nodes.size());
      assertEquals("Public root", nodes.get(0).getContent());
    }

    @Test
    void it_should_update_node_when_node_and_owner_are_valid() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a5400000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a5400000-0000-0000-0000-000000000002");
      UUID nodeId = UUID.fromString("a5400000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      MindmapNode node = new MindmapNode();
      node.setId(nodeId);
      node.setMindmapId(mindmapId);
      node.setContent("Old");
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      when(mindmapNodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      when(mindmapNodeRepository.save(any(MindmapNode.class))).thenAnswer(i -> i.getArgument(0));
      MindmapNodeRequest request =
          MindmapNodeRequest.builder()
              .mindmapId(mindmapId)
              .content("New content")
              .displayOrder(1)
              .build();

      // ===== ACT =====
      var response = mindmapService.updateNode(nodeId, request);

      // ===== ASSERT =====
      assertEquals("New content", response.getContent());
      assertEquals(1, response.getDisplayOrder());
    }

    @Test
    void it_should_export_public_mindmap_as_pdf_when_no_format_is_provided() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("a5500000-0000-0000-0000-000000000001");
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTitle("Public PDF");
      m.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      when(mindmapNodeRepository.findByMindmapId(mindmapId))
          .thenReturn(new java.util.ArrayList<>());

      // ===== ACT =====
      var file = mindmapService.exportPublicMindmap(mindmapId, null);

      // ===== ASSERT =====
      assertEquals("application/pdf", file.contentType());
      assertTrue(file.content().length > 0);
    }

    @Test
    void it_should_get_mindmap_detail_when_owner_requests_existing_mindmap() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a5600000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a5600000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      m.setTitle("Detail map");
      when(mindmapRepository.findByIdWithDetailsAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      MindmapNode root = new MindmapNode();
      root.setId(UUID.fromString("a5600000-0000-0000-0000-000000000003"));
      root.setMindmapId(mindmapId);
      root.setParentId(null);
      root.setContent("Root");
      root.setDisplayOrder(0);
      MindmapNode child = new MindmapNode();
      child.setId(UUID.fromString("a5600000-0000-0000-0000-000000000004"));
      child.setMindmapId(mindmapId);
      child.setParentId(root.getId());
      child.setContent("Child");
      child.setDisplayOrder(1);
      when(mindmapNodeRepository.findByMindmapId(mindmapId))
          .thenReturn(new java.util.ArrayList<>(List.of(root, child)));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));

      // ===== ACT =====
      var detail = mindmapService.getMindmapById(mindmapId);

      // ===== ASSERT =====
      assertEquals("Detail map", detail.getMindmap().getTitle());
      assertEquals(1, detail.getNodes().size());
      assertEquals(1, detail.getNodes().get(0).getChildren().size());
    }

    @Test
    void it_should_throw_access_denied_when_public_mindmap_detail_is_requested_for_draft_status() {
      // ===== ARRANGE =====
      UUID mindmapId = UUID.fromString("a5700000-0000-0000-0000-000000000001");
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setStatus(MindmapStatus.DRAFT);
      when(mindmapRepository.findByIdWithDetailsAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> mindmapService.getPublicMindmapById(mindmapId));
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("additional helper branches")
  class AdditionalHelperBranchTests {

    @Test
    void it_should_return_empty_node_count_map_when_input_ids_are_empty() {
      // ===== ARRANGE =====
      List<UUID> ids = List.of();

      // ===== ACT =====
      Map<?, ?> result =
          invokePrivate("getNodeCountsForMindmaps", new Class<?>[] {List.class}, ids);

      // ===== ASSERT =====
      assertTrue(result.isEmpty());
    }

    @Test
    void it_should_return_empty_teacher_name_map_when_input_mindmaps_are_empty() {
      // ===== ARRANGE =====
      List<Mindmap> mindmaps = List.of();

      // ===== ACT =====
      Map<?, ?> result =
          invokePrivate("getTeacherNamesForMindmaps", new Class<?>[] {List.class}, mindmaps);

      // ===== ASSERT =====
      assertTrue(result.isEmpty());
    }

    @Test
    void it_should_return_empty_lesson_title_map_when_input_mindmaps_are_empty() {
      // ===== ARRANGE =====
      List<Mindmap> mindmaps = List.of();

      // ===== ACT =====
      Map<?, ?> result =
          invokePrivate("getLessonTitlesForMindmaps", new Class<?>[] {List.class}, mindmaps);

      // ===== ASSERT =====
      assertTrue(result.isEmpty());
    }

    @Test
    void it_should_delete_node_when_owner_matches_current_teacher() {
      // ===== ARRANGE =====
      UUID teacherId = UUID.fromString("a5800000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("a5800000-0000-0000-0000-000000000002");
      UUID nodeId = UUID.fromString("a5800000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);

      MindmapNode node = new MindmapNode();
      node.setId(nodeId);
      node.setMindmapId(mindmapId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);

      when(mindmapNodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));

      // ===== ACT =====
      mindmapService.deleteNode(nodeId);

      // ===== ASSERT =====
      verify(mindmapNodeRepository, times(1)).delete(node);
    }

    @Test
    void it_should_load_published_mindmaps_for_student_when_querying_by_lesson() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("a5900000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("a5900000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("a5900000-0000-0000-0000-000000000003");
      mockJwtAuth(studentId);

      Role studentRole = new Role();
      studentRole.setName("STUDENT");
      User student = new User();
      student.setId(studentId);
      student.setRoles(Set.of(studentRole));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

      Lesson lesson = new Lesson();
      lesson.setTitle("Bài 1");
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setStatus(MindmapStatus.PUBLISHED);
      mindmap.setTeacherId(UUID.fromString("a5900000-0000-0000-0000-000000000004"));
      Pageable pageable = PageRequest.of(0, 10);
      when(mindmapRepository.findByLessonIdAndStatusWithDetailsAndNotDeleted(
              lessonId, MindmapStatus.PUBLISHED, pageable))
          .thenReturn(new PageImpl<>(List.of(mindmap), pageable, 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId)))
          .thenReturn(List.of());

      // ===== ACT =====
      var page = mindmapService.getMindmapsByLesson(lessonId, pageable);

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());
      verify(mindmapRepository, times(1))
          .findByLessonIdAndStatusWithDetailsAndNotDeleted(lessonId, MindmapStatus.PUBLISHED, pageable);
    }
  }

  @Nested
  @DisplayName("normalizeExportFormat()")
  class NormalizeExportFormatTests {

    /**
     * Normal case: Default export format to PDF for null input.
     *
     * <p>Input:
     * <ul>
     *   <li>format: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>format == null TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return {@code pdf}</li>
     * </ul>
     */
    @Test
    void it_should_return_pdf_when_format_is_null() {
      // ===== ARRANGE =====
      String format = null;

      // ===== ACT =====
      String actual = invokePrivate("normalizeExportFormat", new Class<?>[] {String.class}, format);

      // ===== ASSERT =====
      assertEquals("pdf", actual);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    /**
     * Abnormal case: Reject unsupported export format.
     *
     * <p>Input:
     * <ul>
     *   <li>format: "xlsx"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>invalid-format guard TRUE branch (throw exception)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} with {@code INVALID_FILE_FORMAT}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_format_is_not_pdf_or_png() {
      // ===== ARRANGE =====
      String format = "xlsx";

      // ===== ACT & ASSERT =====
      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("normalizeExportFormat", new Class<?>[] {String.class}, format));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.INVALID_FILE_FORMAT, ((AppException) target).getErrorCode());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_return_pdf_when_format_is_blank() {
      String actual = invokePrivate("normalizeExportFormat", new Class<?>[] {String.class}, "   ");
      assertEquals("pdf", actual);
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("findCycleNodes()")
  class FindCycleNodesTests {

    /**
     * Normal case: Detect cycle nodes in parent graph.
     *
     * <p>Input:
     * <ul>
     *   <li>parent map: A -&gt; B, B -&gt; C, C -&gt; B (cycle), D -&gt; null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>state == 1 TRUE branch to capture cycle range</li>
     *   <li>normal visited path finalize branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return set contains B and C, excludes D</li>
     * </ul>
     */
    @Test
    void it_should_return_cycle_node_ids_when_parent_graph_contains_cycle() {
      // ===== ARRANGE =====
      UUID a = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
      UUID b = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
      UUID c = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
      UUID d = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
      Map<UUID, UUID> parentMap = new HashMap<>();
      parentMap.put(a, b);
      parentMap.put(b, c);
      parentMap.put(c, b);
      parentMap.put(d, null);

      // ===== ACT =====
      Set<UUID> cycleNodes =
          invokePrivate("findCycleNodes", new Class<?>[] {Map.class}, parentMap);

      // ===== ASSERT =====
      assertTrue(cycleNodes.contains(b));
      assertTrue(cycleNodes.contains(c));
      assertTrue(!cycleNodes.contains(d));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("parseColor()")
  class ParseColorTests {

    /**
     * Normal case: Fallback to default color when color text is invalid.
     *
     * <p>Input:
     * <ul>
     *   <li>colorHex: "invalid-color-value"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>decode exception branch TRUE</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return default color rgb(76,110,245)</li>
     * </ul>
     */
    @Test
    void it_should_return_default_color_when_color_hex_cannot_be_decoded() {
      // ===== ARRANGE =====
      String invalidColor = "invalid-color-value";

      // ===== ACT =====
      Color color = invokePrivate("parseColor", new Class<?>[] {String.class}, invalidColor);

      // ===== ASSERT =====
      assertEquals(76, color.getRed());
      assertEquals(110, color.getGreen());
      assertEquals(245, color.getBlue());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    /**
     * Normal case: Parse valid hex color.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return decoded color from #4A90E2</li>
     * </ul>
     */
    @Test
    void it_should_decode_hex_color_when_input_is_valid() {
      // ===== ARRANGE =====
      String validHex = "#4A90E2";

      // ===== ACT =====
      Color color = invokePrivate("parseColor", new Class<?>[] {String.class}, validHex);

      // ===== ASSERT =====
      assertEquals(74, color.getRed());
      assertEquals(144, color.getGreen());
      assertEquals(226, color.getBlue());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_return_default_color_when_input_is_null() {
      Color color = invokePrivate("parseColor", new Class<?>[] {String.class}, (Object) null);
      assertEquals(76, color.getRed());
      assertEquals(110, color.getGreen());
      assertEquals(245, color.getBlue());
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("text and file-name helpers")
  class TextAndFileNameHelperTests {

    @Test
    void it_should_replace_unsafe_characters_when_sanitizing_file_name() {
      // ===== ARRANGE =====
      String raw = "Mindmap toán 12 - chương #1.pdf";

      // ===== ACT =====
      String sanitized = invokePrivate("sanitizeFileName", new Class<?>[] {String.class}, raw);

      // ===== ASSERT =====
      assertEquals("Mindmap_to_n_12_-_ch__ng__1.pdf", sanitized);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_return_empty_placeholder_when_text_is_blank() {
      // ===== ARRANGE =====
      String blank = "   ";

      // ===== ACT =====
      String safe = invokePrivate("safeText", new Class<?>[] {String.class}, blank);

      // ===== ASSERT =====
      assertEquals("(empty)", safe);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_trim_and_compact_spaces_when_text_contains_extra_whitespace() {
      // ===== ARRANGE =====
      String text = "  Hàm   số   bậc   hai  ";

      // ===== ACT =====
      String safe = invokePrivate("safeText", new Class<?>[] {String.class}, text);

      // ===== ASSERT =====
      assertEquals("Hàm số bậc hai", safe);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_return_empty_placeholder_when_text_is_null() {
      String safe = invokePrivate("safeText", new Class<?>[] {String.class}, (Object) null);
      assertEquals("(empty)", safe);
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("buildMindmapGenerationPrompt()")
  class BuildMindmapGenerationPromptTests {

    @Test
    void it_should_embed_prompt_and_levels_when_building_ai_instruction() {
      // ===== ARRANGE =====
      String userPrompt = "Vector and matrix basics";
      int levels = 4;

      // ===== ACT =====
      String prompt =
          invokePrivate(
              "buildMindmapGenerationPrompt",
              new Class<?>[] {String.class, int.class},
              userPrompt,
              levels);

      // ===== ASSERT =====
      assertTrue(prompt.contains(userPrompt));
      assertTrue(prompt.contains("EXACTLY 4 levels deep"));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("render and export helpers")
  class RenderAndExportHelperTests {

    @Test
    void it_should_render_non_empty_image_when_export_lines_are_provided() throws Exception {
      // ===== ARRANGE =====
      Class<?> lineClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$ExportNodeLine");
      var ctor = lineClass.getDeclaredConstructor(int.class, String.class, String.class);
      ctor.setAccessible(true);
      Object line =
          ctor.newInstance(0, "Root", "#4A90E2");
      List<Object> lines = List.of(line);

      // ===== ACT =====
      java.awt.image.BufferedImage image =
          invokePrivate(
              "renderMindmapImage", new Class<?>[] {String.class, List.class}, "Algebra", lines);

      // ===== ASSERT =====
      assertNotNull(image);
      assertTrue(image.getWidth() > 0);
      assertTrue(image.getHeight() > 0);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_convert_rendered_image_to_png_bytes() {
      // ===== ARRANGE =====
      java.awt.image.BufferedImage image =
          new java.awt.image.BufferedImage(120, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);

      // ===== ACT =====
      byte[] bytes = invokePrivate("toPngBytes", new Class<?>[] {java.awt.image.BufferedImage.class}, image);

      // ===== ASSERT =====
      assertNotNull(bytes);
      assertTrue(bytes.length > 0);

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("findCycleNodes() additional")
  class FindCycleNodesAdditionalTests {

    @Test
    void it_should_return_empty_set_when_parent_graph_has_no_cycle() {
      // ===== ARRANGE =====
      UUID a = UUID.fromString("11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
      UUID b = UUID.fromString("22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
      UUID c = UUID.fromString("33333333-cccc-cccc-cccc-cccccccccccc");
      Map<UUID, UUID> parentMap = new HashMap<>();
      parentMap.put(a, null);
      parentMap.put(b, a);
      parentMap.put(c, b);

      // ===== ACT =====
      Set<UUID> cycleNodes =
          invokePrivate("findCycleNodes", new Class<?>[] {Map.class}, parentMap);

      // ===== ASSERT =====
      assertTrue(cycleNodes.isEmpty());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("buildExportLines()")
  class BuildExportLinesTests {

    @Test
    void it_should_treat_missing_parent_node_as_root_line() {
      // ===== ARRANGE =====
      UUID missingParent = UUID.fromString("77777777-7777-7777-7777-777777777777");
      MindmapNode node = new MindmapNode();
      node.setId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
      node.setParentId(missingParent);
      node.setContent("Orphan node");
      node.setColor("#50C878");
      node.setDisplayOrder(0);

      // ===== ACT =====
      List<?> lines = invokePrivate("buildExportLines", new Class<?>[] {List.class}, List.of(node));

      // ===== ASSERT =====
      assertEquals(1, lines.size());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }

  @Nested
  @DisplayName("additional public and auth branches")
  class AdditionalPublicAndAuthBranchTests {

    @Test
    void it_should_throw_not_a_teacher_when_create_mindmap_called_by_student() {
      UUID userId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
      mockJwtAuth(userId);
      Role studentRole = new Role();
      studentRole.setName("STUDENT");
      User student = new User();
      student.setId(userId);
      student.setRoles(Set.of(studentRole));
      when(userRepository.findById(userId)).thenReturn(Optional.of(student));

      MindmapRequest request = new MindmapRequest();
      request.setTitle("Denied");

      AppException ex = assertThrows(AppException.class, () -> mindmapService.createMindmap(request));
      assertEquals(ErrorCode.NOT_A_TEACHER, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_create_mindmap_has_unknown_lesson() {
      UUID teacherId = UUID.fromString("b1100000-0000-0000-0000-000000000001");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      UUID lessonId = UUID.fromString("b1100000-0000-0000-0000-000000000002");
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

      MindmapRequest request = new MindmapRequest();
      request.setTitle("Lesson check");
      request.setLessonId(lessonId);

      AppException ex = assertThrows(AppException.class, () -> mindmapService.createMindmap(request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_invalid_structure_when_generate_mindmap_parsing_fails() throws Exception {
      UUID teacherId = UUID.fromString("b1200000-0000-0000-0000-000000000001");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(geminiService.sendMessage(any())).thenReturn("{bad json}");
      when(objectMapper.readValue(any(String.class), any(Class.class)))
          .thenThrow(new RuntimeException("cannot parse"));

      GenerateMindmapRequest request =
          GenerateMindmapRequest.builder().prompt("x").title("t").levels(2).build();

      AppException ex = assertThrows(AppException.class, () -> mindmapService.generateMindmap(request));
      assertEquals(ErrorCode.INVALID_MINDMAP_STRUCTURE, ex.getErrorCode());
    }

    @Test
    void it_should_use_teacher_branch_when_get_mindmaps_by_lesson_called_by_teacher() {
      UUID teacherId = UUID.fromString("b1300000-0000-0000-0000-000000000001");
      UUID lessonId = UUID.fromString("b1300000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("b1300000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      Lesson lesson = new Lesson();
      lesson.setId(lessonId);
      lesson.setTitle("L");
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      PageRequest pageable = PageRequest.of(0, 5);
      when(mindmapRepository.findByLessonIdWithDetailsAndNotDeleted(lessonId, pageable))
          .thenReturn(new PageImpl<>(List.of(m), pageable, 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of());

      var page = mindmapService.getMindmapsByLesson(lessonId, pageable);
      assertEquals(1, page.getTotalElements());
      verify(mindmapRepository, times(1)).findByLessonIdWithDetailsAndNotDeleted(lessonId, pageable);
    }

    @Test
    void it_should_use_lesson_filter_branch_when_get_my_mindmaps_has_lesson_id() {
      UUID teacherId = UUID.fromString("b1400000-0000-0000-0000-000000000001");
      UUID lessonId = UUID.fromString("b1400000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("b1400000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      PageRequest pageable = PageRequest.of(0, 5);
      when(mindmapRepository.findByTeacherIdAndLessonIdWithDetailsAndNotDeleted(teacherId, lessonId, pageable))
          .thenReturn(new PageImpl<>(List.of(mindmap), pageable, 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of());
      when(userRepository.findAllById(Set.of(teacherId))).thenReturn(List.of(buildTeacher(teacherId)));

      var page = mindmapService.getMyMindmaps(lessonId, pageable);
      assertEquals(1, page.getTotalElements());
      verify(mindmapRepository, times(1))
          .findByTeacherIdAndLessonIdWithDetailsAndNotDeleted(teacherId, lessonId, pageable);
    }

    @Test
    void it_should_throw_invalid_file_format_when_export_mindmap_has_unknown_format() {
      UUID teacherId = UUID.fromString("b1500000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("b1500000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(teacherId);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));

      AppException ex =
          assertThrows(AppException.class, () -> mindmapService.exportMindmap(mindmapId, "xlsx"));
      assertEquals(ErrorCode.INVALID_FILE_FORMAT, ex.getErrorCode());
    }

    @Test
    void it_should_allow_non_owner_admin_for_export_mindmap() {
      UUID adminId = UUID.fromString("b1600000-0000-0000-0000-000000000001");
      UUID ownerId = UUID.fromString("b1600000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("b1600000-0000-0000-0000-000000000003");
      Jwt jwt =
          new Jwt(
              "token",
              Instant.now(),
              Instant.now().plusSeconds(600),
              Map.of("alg", "none"),
              Map.of("sub", adminId.toString(), "scope", "ROLE_ADMIN"));
      SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
      Mindmap mindmap = new Mindmap();
      mindmap.setId(mindmapId);
      mindmap.setTeacherId(ownerId);
      mindmap.setStatus(MindmapStatus.PUBLISHED);
      mindmap.setTitle("Admin Export");
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(mindmap));
      Role adminRole = new Role();
      adminRole.setName("ADMIN");
      User admin = new User();
      admin.setId(adminId);
      admin.setRoles(Set.of(adminRole));
      when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
      when(mindmapNodeRepository.findByMindmapId(mindmapId)).thenReturn(new java.util.ArrayList<>());

      var file = mindmapService.exportMindmap(mindmapId, "pdf");
      assertEquals("application/pdf", file.contentType());
    }
  }

  @Nested
  @DisplayName("additional helper branches second batch")
  class AdditionalHelperSecondBatchTests {

    @Test
    void it_should_throw_when_get_current_user_id_without_jwt_authentication() {
      SecurityContextHolder.clearContext();
      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("getCurrentUserId", new Class<?>[] {}));
      Throwable target =
          ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof IllegalStateException);
    }

    @Test
    void it_should_return_empty_when_parse_color_input_is_blank() {
      Color color = invokePrivate("parseColor", new Class<?>[] {String.class}, " ");
      assertEquals(76, color.getRed());
      assertEquals(110, color.getGreen());
      assertEquals(245, color.getBlue());
    }

    @Test
    void it_should_normalize_export_format_when_input_has_spaces_and_uppercase() {
      String format = invokePrivate("normalizeExportFormat", new Class<?>[] {String.class}, " PNG ");
      assertEquals("png", format);
    }

    @Test
    void it_should_populate_teacher_names_from_missing_teacher_ids() {
      UUID teacherId = UUID.fromString("b1700000-0000-0000-0000-000000000001");
      Mindmap m = new Mindmap();
      m.setTeacherId(teacherId);
      m.setId(UUID.fromString("b1700000-0000-0000-0000-000000000002"));
      when(userRepository.findAllById(Set.of(teacherId))).thenReturn(List.of(buildTeacher(teacherId)));

      @SuppressWarnings("unchecked")
      Map<UUID, String> names =
          invokePrivate("getTeacherNamesForMindmaps", new Class<?>[] {List.class}, List.of(m));

      assertEquals("Nguyen Van A", names.get(teacherId));
    }

    @Test
    void it_should_populate_lesson_titles_from_missing_lesson_ids() {
      UUID lessonId = UUID.fromString("b1800000-0000-0000-0000-000000000001");
      Mindmap m = new Mindmap();
      m.setLessonId(lessonId);
      Lesson lesson = new Lesson();
      lesson.setId(lessonId);
      lesson.setTitle("Đại số nâng cao");
      when(lessonRepository.findAllById(Set.of(lessonId))).thenReturn(List.of(lesson));

      @SuppressWarnings("unchecked")
      Map<UUID, String> titles =
          invokePrivate("getLessonTitlesForMindmaps", new Class<?>[] {List.class}, List.of(m));

      assertEquals("Đại số nâng cao", titles.get(lessonId));
    }

    @Test
    void it_should_collect_node_counts_projection_for_mindmap_ids() {
      UUID mindmapId = UUID.fromString("b1900000-0000-0000-0000-000000000001");
      MindmapNodeRepository.NodeCountProjection projection =
          new MindmapNodeRepository.NodeCountProjection() {
            @Override
            public UUID getMindmapId() {
              return mindmapId;
            }

            @Override
            public Long getCount() {
              return 5L;
            }
          };
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of(projection));

      @SuppressWarnings("unchecked")
      Map<UUID, Long> counts =
          invokePrivate("getNodeCountsForMindmaps", new Class<?>[] {List.class}, List.of(mindmapId));

      assertEquals(5L, counts.get(mindmapId));
    }
  }

  @Nested
  @DisplayName("third branch push set")
  class ThirdBranchPushTests {

    @Test
    void it_should_throw_not_found_when_get_public_mindmap_by_id_missing() {
      UUID id = UUID.fromString("b2000000-0000-0000-0000-000000000001");
      when(mindmapRepository.findByIdWithDetailsAndNotDeleted(id)).thenReturn(Optional.empty());

      AppException ex = assertThrows(AppException.class, () -> mindmapService.getPublicMindmapById(id));
      assertEquals(ErrorCode.MINDMAP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_get_public_nodes_for_draft_mindmap() {
      UUID id = UUID.fromString("b2100000-0000-0000-0000-000000000001");
      Mindmap m = new Mindmap();
      m.setId(id);
      m.setStatus(MindmapStatus.DRAFT);
      when(mindmapRepository.findByIdAndNotDeleted(id)).thenReturn(Optional.of(m));

      AppException ex = assertThrows(AppException.class, () -> mindmapService.getPublicNodesByMindmap(id));
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_teacher_reads_nodes_of_other_teacher_without_admin() {
      UUID me = UUID.fromString("b2200000-0000-0000-0000-000000000001");
      UUID owner = UUID.fromString("b2200000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("b2200000-0000-0000-0000-000000000003");
      mockJwtAuth(me);
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(owner);
      m.setStatus(MindmapStatus.PUBLISHED);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      when(userRepository.findById(me)).thenReturn(Optional.of(buildTeacher(me)));

      AppException ex = assertThrows(AppException.class, () -> mindmapService.getNodesByMindmap(mindmapId));
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_create_node_parent_not_exists() {
      UUID teacherId = UUID.fromString("b2300000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("b2300000-0000-0000-0000-000000000002");
      UUID parentId = UUID.fromString("b2300000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      when(mindmapNodeRepository.findByIdAndMindmapId(parentId, mindmapId)).thenReturn(Optional.empty());
      MindmapNodeRequest req =
          MindmapNodeRequest.builder().mindmapId(mindmapId).parentId(parentId).content("child").build();

      AppException ex = assertThrows(AppException.class, () -> mindmapService.createNode(req));
      assertEquals(ErrorCode.MINDMAP_NODE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_update_node_id_missing() {
      UUID nodeId = UUID.fromString("b2400000-0000-0000-0000-000000000001");
      when(mindmapNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> mindmapService.updateNode(nodeId, MindmapNodeRequest.builder().content("x").build()));
      assertEquals(ErrorCode.MINDMAP_NODE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_delete_node_id_missing() {
      UUID nodeId = UUID.fromString("b2500000-0000-0000-0000-000000000001");
      when(mindmapNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

      AppException ex = assertThrows(AppException.class, () -> mindmapService.deleteNode(nodeId));
      assertEquals(ErrorCode.MINDMAP_NODE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_parse_mindmap_from_ai_when_wrapped_in_json_code_fence() throws Exception {
      String payload = "{\"title\":\"T\",\"description\":\"D\",\"nodes\":[]}";
      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      Object structure = structureClass.getDeclaredConstructor().newInstance();
      when(objectMapper.readValue(any(String.class), any(Class.class))).thenReturn(structure);
      @SuppressWarnings("unchecked")
      Object parsed =
          invokePrivate(
              "parseMindmapFromAI", new Class<?>[] {String.class}, "```json\n" + payload + "\n```");
      assertNotNull(parsed);
    }

    @Test
    void it_should_parse_mindmap_from_ai_when_wrapped_in_plain_code_fence() throws Exception {
      String payload = "{\"title\":\"T2\",\"description\":\"D2\",\"nodes\":[]}";
      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      Object structure = structureClass.getDeclaredConstructor().newInstance();
      when(objectMapper.readValue(any(String.class), any(Class.class))).thenReturn(structure);
      @SuppressWarnings("unchecked")
      Object parsed =
          invokePrivate(
              "parseMindmapFromAI", new Class<?>[] {String.class}, "```\n" + payload + "\n```");
      assertNotNull(parsed);
    }

    @Test
    void it_should_validate_owner_or_admin_allows_owner_immediately() {
      UUID owner = UUID.fromString("b2600000-0000-0000-0000-000000000001");
      invokePrivate("validateOwnerOrAdmin", new Class<?>[] {UUID.class, UUID.class}, owner, owner);
      verifyNoInteractions(userRepository);
    }

    @Test
    void it_should_throw_user_not_found_when_validate_owner_or_admin_user_missing() {
      UUID owner = UUID.fromString("b2700000-0000-0000-0000-000000000001");
      UUID current = UUID.fromString("b2700000-0000-0000-0000-000000000002");
      when(userRepository.findById(current)).thenReturn(Optional.empty());

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateOwnerOrAdmin", new Class<?>[] {UUID.class, UUID.class}, owner, current));
      Throwable target =
          ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.USER_NOT_EXISTED, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_validate_owner_or_admin_non_admin_user() {
      UUID owner = UUID.fromString("b2800000-0000-0000-0000-000000000001");
      UUID current = UUID.fromString("b2800000-0000-0000-0000-000000000002");
      when(userRepository.findById(current)).thenReturn(Optional.of(buildTeacher(current)));

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateOwnerOrAdmin", new Class<?>[] {UUID.class, UUID.class}, owner, current));
      Throwable target =
          ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.MINDMAP_ACCESS_DENIED, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_return_false_when_is_student_for_teacher_role() {
      UUID userId = UUID.fromString("b2900000-0000-0000-0000-000000000001");
      when(userRepository.findById(userId)).thenReturn(Optional.of(buildTeacher(userId)));

      boolean student = invokePrivate("isStudent", new Class<?>[] {UUID.class}, userId);
      assertTrue(!student);
    }

    @Test
    void it_should_generate_mindmap_successfully_with_default_levels_and_invalid_lesson_id_format()
        throws Exception {
      UUID teacherId = UUID.fromString("b3000000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("b3000000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(geminiService.sendMessage(any())).thenReturn("{\"ok\":true}");

      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Object structure = structureClass.getDeclaredConstructor().newInstance();
      Object root = nodeClass.getDeclaredConstructor().newInstance();
      Object child = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(root, "Root");
      nodeClass.getMethod("setIcon", String.class).invoke(root, "lightbulb");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(root, 0);
      nodeClass.getMethod("setContent", String.class).invoke(child, "Child");
      nodeClass.getMethod("setIcon", String.class).invoke(child, "book");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(child, 0);
      nodeClass.getMethod("setChildren", List.class).invoke(child, List.of());
      nodeClass.getMethod("setChildren", List.class).invoke(root, List.of(child));
      structureClass.getMethod("setTitle", String.class).invoke(structure, "AI title");
      structureClass.getMethod("setDescription", String.class).invoke(structure, "AI description");
      structureClass.getMethod("setNodes", List.class).invoke(structure, List.of(root));
      when(objectMapper.readValue(any(String.class), any(Class.class))).thenReturn(structure);

      when(mindmapRepository.save(any(Mindmap.class)))
          .thenAnswer(
              inv -> {
                Mindmap m = inv.getArgument(0);
                if (m.getId() == null) {
                  m.setId(mindmapId);
                }
                return m;
              });
      Mindmap persisted = new Mindmap();
      persisted.setId(mindmapId);
      persisted.setTeacherId(teacherId);
      persisted.setStatus(MindmapStatus.DRAFT);
      persisted.setTitle("AI title");
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(persisted));
      when(mindmapNodeRepository.save(any(MindmapNode.class)))
          .thenAnswer(
              inv -> {
                MindmapNode n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
              });
      when(mindmapNodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
      when(mindmapNodeRepository.findByMindmapId(mindmapId)).thenReturn(new java.util.ArrayList<>());

      GenerateMindmapRequest request =
          GenerateMindmapRequest.builder()
              .prompt("Generate a map")
              .title(null)
              .levels(null)
              .lessonId("invalid-uuid")
              .build();

      var response = mindmapService.generateMindmap(request);
      assertNotNull(response);
      assertNotNull(response.getMindmap());
      assertEquals(mindmapId, response.getMindmap().getId());
      verify(userSubscriptionService, times(1)).consumeMyTokens(2, "MINDMAP");
    }

    @Test
    void it_should_update_mindmap_lesson_when_request_contains_valid_lesson() {
      UUID teacherId = UUID.fromString("b3100000-0000-0000-0000-000000000001");
      UUID mindmapId = UUID.fromString("b3100000-0000-0000-0000-000000000002");
      UUID lessonId = UUID.fromString("b3100000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      m.setTitle("Keep title");
      when(mindmapRepository.findByIdAndNotDeleted(mindmapId)).thenReturn(Optional.of(m));
      Lesson lesson = new Lesson();
      lesson.setId(lessonId);
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
      when(mindmapRepository.save(any(Mindmap.class))).thenAnswer(inv -> inv.getArgument(0));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(mindmapNodeRepository.countByMindmapId(mindmapId)).thenReturn(0L);

      MindmapRequest request = new MindmapRequest();
      request.setLessonId(lessonId);
      request.setTitle(null);
      request.setDescription("Updated desc");

      MindmapResponse response = mindmapService.updateMindmap(mindmapId, request);
      assertEquals(lessonId, response.getLessonId());
      assertEquals("Keep title", response.getTitle());
    }

    @Test
    void it_should_throw_not_found_when_export_public_mindmap_id_does_not_exist() {
      UUID id = UUID.fromString("b3200000-0000-0000-0000-000000000001");
      when(mindmapRepository.findByIdAndNotDeleted(id)).thenReturn(Optional.empty());

      AppException ex = assertThrows(AppException.class, () -> mindmapService.exportPublicMindmap(id, "pdf"));
      assertEquals(ErrorCode.MINDMAP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_user_not_exist_when_create_mindmap_has_unknown_user() {
      UUID userId = UUID.fromString("b3300000-0000-0000-0000-000000000001");
      mockJwtAuth(userId);
      when(userRepository.findById(userId)).thenReturn(Optional.empty());
      MindmapRequest request = new MindmapRequest();
      request.setTitle("x");

      AppException ex = assertThrows(AppException.class, () -> mindmapService.createMindmap(request));
      assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
    }

    @Test
    void it_should_create_mindmap_with_existing_lesson_id() {
      UUID teacherId = UUID.fromString("b3400000-0000-0000-0000-000000000001");
      UUID lessonId = UUID.fromString("b3400000-0000-0000-0000-000000000002");
      UUID mindmapId = UUID.fromString("b3400000-0000-0000-0000-000000000003");
      mockJwtAuth(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      Lesson lesson = new Lesson();
      lesson.setId(lessonId);
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
      when(mindmapRepository.save(any(Mindmap.class)))
          .thenAnswer(
              inv -> {
                Mindmap m = inv.getArgument(0);
                m.setId(mindmapId);
                return m;
              });
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId)));
      when(mindmapNodeRepository.countByMindmapId(mindmapId)).thenReturn(0L);

      MindmapRequest req = new MindmapRequest();
      req.setTitle("With lesson");
      req.setLessonId(lessonId);

      MindmapResponse response = mindmapService.createMindmap(req);
      assertEquals(lessonId, response.getLessonId());
    }

    @Test
    void it_should_throw_not_found_when_get_mindmaps_by_lesson_has_unknown_lesson() {
      UUID teacherId = UUID.fromString("b3500000-0000-0000-0000-000000000001");
      UUID lessonId = UUID.fromString("b3500000-0000-0000-0000-000000000002");
      mockJwtAuth(teacherId);
      when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> mindmapService.getMindmapsByLesson(lessonId, PageRequest.of(0, 5)));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_fetch_public_mindmaps_when_name_filter_is_null() {
      UUID mindmapId = UUID.fromString("b3600000-0000-0000-0000-000000000001");
      UUID teacherId = UUID.fromString("b3600000-0000-0000-0000-000000000002");
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      m.setTitle("No name filter");
      when(mindmapRepository.findPublicWithFilters(MindmapStatus.PUBLISHED, null, null, PageRequest.of(0, 5)))
          .thenReturn(new PageImpl<>(List.of(m), PageRequest.of(0, 5), 1));
      when(mindmapNodeRepository.countByMindmapIds(List.of(mindmapId))).thenReturn(List.of());
      when(userRepository.findAllById(Set.of(teacherId))).thenReturn(List.of(buildTeacher(teacherId)));

      var page = mindmapService.getPublicMindmaps(null, null, PageRequest.of(0, 5));
      assertEquals(1, page.getTotalElements());
    }

    @Test
    void it_should_throw_user_not_exist_when_is_student_receives_unknown_user() {
      UUID userId = UUID.fromString("b3700000-0000-0000-0000-000000000001");
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("isStudent", new Class<?>[] {UUID.class}, userId));
      Throwable target =
          ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.USER_NOT_EXISTED, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_use_last_level_color_when_create_nodes_from_structure_exceeds_palette() throws Exception {
      UUID mindmapId = UUID.fromString("b3800000-0000-0000-0000-000000000001");
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Object node = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(node, "Deep node");
      nodeClass.getMethod("setIcon", String.class).invoke(node, "book");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(node, 0);
      nodeClass.getMethod("setChildren", List.class).invoke(node, (Object) null);
      when(mindmapNodeRepository.save(any(MindmapNode.class)))
          .thenAnswer(
              inv -> {
                MindmapNode n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
              });

      @SuppressWarnings("unchecked")
      List<MindmapNode> created =
          invokePrivate(
              "createNodesFromStructure",
              new Class<?>[] {UUID.class, List.class, UUID.class, int.class},
              mindmapId,
              List.of(node),
              null,
              10);

      assertEquals(1, created.size());
      assertEquals("#FF8C94", created.get(0).getColor());
    }

    @Test
    void it_should_build_lesson_title_map_for_attached_missing_and_null_lesson_ids() {
      UUID lessonAttached = UUID.fromString("b3900000-0000-0000-0000-000000000001");
      UUID lessonMissing = UUID.fromString("b3900000-0000-0000-0000-000000000002");

      Mindmap withAttached = new Mindmap();
      withAttached.setLessonId(lessonAttached);
      Lesson attached = new Lesson();
      attached.setId(lessonAttached);
      attached.setTitle("Attached");
      withAttached.setLesson(attached);

      Mindmap withMissing = new Mindmap();
      withMissing.setLessonId(lessonMissing);

      Mindmap noLesson = new Mindmap();
      noLesson.setLessonId(null);

      Lesson fetched = new Lesson();
      fetched.setId(lessonMissing);
      fetched.setTitle("Fetched");
      when(lessonRepository.findAllById(Set.of(lessonMissing))).thenReturn(List.of(fetched));

      @SuppressWarnings("unchecked")
      Map<UUID, String> titles =
          invokePrivate(
              "getLessonTitlesForMindmaps",
              new Class<?>[] {List.class},
              List.of(withAttached, withMissing, noLesson));

      assertEquals("Attached", titles.get(lessonAttached));
      assertEquals("Fetched", titles.get(lessonMissing));
    }

    @Test
    void it_should_default_teacher_name_to_unknown_in_batch_map_response_when_missing() {
      UUID mindmapId = UUID.fromString("b3a00000-0000-0000-0000-000000000001");
      UUID teacherId = UUID.fromString("b3a00000-0000-0000-0000-000000000002");
      Mindmap m = new Mindmap();
      m.setId(mindmapId);
      m.setTeacherId(teacherId);
      m.setLessonId(null);
      m.setTitle("Batch map");
      m.setStatus(MindmapStatus.DRAFT);

      MindmapResponse response =
          invokePrivate(
              "mapToResponseWithNodeCount",
              new Class<?>[] {Mindmap.class, int.class, Map.class, Map.class},
              m,
              3,
              Map.of(),
              Map.of());

      assertEquals("Unknown", response.getTeacherName());
      assertEquals(3, response.getNodeCount());
    }
  }

  @Nested
  @DisplayName("inner structure classes")
  class InnerStructureClassTests {

    @Test
    void it_should_cover_lombok_data_contract_for_node_structure() throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Object first = nodeClass.getDeclaredConstructor().newInstance();
      Object second = nodeClass.getDeclaredConstructor().newInstance();
      Object third = nodeClass.getDeclaredConstructor().newInstance();
      Object child = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(first, "Phương trình bậc hai");
      nodeClass.getMethod("setColor", String.class).invoke(first, "#4A90E2");
      nodeClass.getMethod("setIcon", String.class).invoke(first, "book");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(first, 1);
      nodeClass.getMethod("setChildren", List.class).invoke(first, List.of(child));

      nodeClass.getMethod("setContent", String.class).invoke(second, "Phương trình bậc hai");
      nodeClass.getMethod("setColor", String.class).invoke(second, "#4A90E2");
      nodeClass.getMethod("setIcon", String.class).invoke(second, "book");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(second, 1);
      nodeClass.getMethod("setChildren", List.class).invoke(second, List.of(child));

      nodeClass.getMethod("setContent", String.class).invoke(third, "Lượng giác");
      nodeClass.getMethod("setColor", String.class).invoke(third, "#50C878");
      nodeClass.getMethod("setIcon", String.class).invoke(third, "star");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(third, 2);
      nodeClass.getMethod("setChildren", List.class).invoke(third, List.of());

      // ===== ACT =====
      String firstContent = (String) nodeClass.getMethod("getContent").invoke(first);
      Integer firstDisplayOrder = (Integer) nodeClass.getMethod("getDisplayOrder").invoke(first);
      int firstHash = first.hashCode();
      int secondHash = second.hashCode();
      String firstText = first.toString();

      // ===== ASSERT =====
      assertEquals("Phương trình bậc hai", firstContent);
      assertEquals(1, firstDisplayOrder);
      assertEquals(first, first);
      assertEquals(first, second);
      assertEquals(second, first);
      assertNotEquals(first, third);
      assertNotEquals(first, null);
      assertNotEquals(first, "not-node-structure");
      assertEquals(firstHash, secondHash);
      assertTrue(firstText.contains("NodeStructure"));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_cover_no_args_and_all_args_constructor_for_node_structure() throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Object child = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(child, "Nút con");
      var allArgsCtor =
          nodeClass.getDeclaredConstructor(
              String.class, String.class, String.class, Integer.class, List.class);

      // ===== ACT =====
      Object created =
          allArgsCtor.newInstance("Nút gốc", "#FFD93D", "lightbulb", 0, List.of(child));

      // ===== ASSERT =====
      assertEquals("Nút gốc", nodeClass.getMethod("getContent").invoke(created));
      assertEquals("#FFD93D", nodeClass.getMethod("getColor").invoke(created));
      assertEquals("lightbulb", nodeClass.getMethod("getIcon").invoke(created));
      assertEquals(0, nodeClass.getMethod("getDisplayOrder").invoke(created));
      assertEquals(1, ((List<?>) nodeClass.getMethod("getChildren").invoke(created)).size());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_cover_lombok_data_contract_for_mindmap_structure() throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      Object node = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(node, "Giới hạn hàm số");
      nodeClass.getMethod("setDisplayOrder", Integer.class).invoke(node, 0);
      nodeClass.getMethod("setChildren", List.class).invoke(node, List.of());

      Object first = structureClass.getDeclaredConstructor().newInstance();
      Object second = structureClass.getDeclaredConstructor().newInstance();
      Object third = structureClass.getDeclaredConstructor().newInstance();
      structureClass.getMethod("setTitle", String.class).invoke(first, "Toán 12");
      structureClass.getMethod("setDescription", String.class).invoke(first, "Sơ đồ tư duy học kỳ 2");
      structureClass.getMethod("setNodes", List.class).invoke(first, List.of(node));

      structureClass.getMethod("setTitle", String.class).invoke(second, "Toán 12");
      structureClass.getMethod("setDescription", String.class).invoke(second, "Sơ đồ tư duy học kỳ 2");
      structureClass.getMethod("setNodes", List.class).invoke(second, List.of(node));

      structureClass.getMethod("setTitle", String.class).invoke(third, "Đại số tuyến tính");
      structureClass.getMethod("setDescription", String.class).invoke(third, "Ma trận và định thức");
      structureClass.getMethod("setNodes", List.class).invoke(third, List.of());

      // ===== ACT =====
      String title = (String) structureClass.getMethod("getTitle").invoke(first);
      int firstHash = first.hashCode();
      int secondHash = second.hashCode();
      String firstText = first.toString();

      // ===== ASSERT =====
      assertEquals("Toán 12", title);
      assertEquals(first, first);
      assertEquals(first, second);
      assertEquals(second, first);
      assertNotEquals(first, third);
      assertNotEquals(first, null);
      assertNotEquals(first, "not-mindmap-structure");
      assertEquals(firstHash, secondHash);
      assertTrue(firstText.contains("MindmapStructure"));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_cover_all_args_constructor_for_mindmap_structure() throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      Object node = nodeClass.getDeclaredConstructor().newInstance();
      nodeClass.getMethod("setContent", String.class).invoke(node, "Hệ tọa độ Oxy");
      nodeClass.getMethod("setChildren", List.class).invoke(node, List.of());
      var allArgsCtor =
          structureClass.getDeclaredConstructor(String.class, String.class, List.class);

      // ===== ACT =====
      Object created = allArgsCtor.newInstance("Hình học phẳng", "Các chuyên đề nền tảng", List.of(node));

      // ===== ASSERT =====
      assertEquals("Hình học phẳng", structureClass.getMethod("getTitle").invoke(created));
      assertEquals("Các chuyên đề nền tảng", structureClass.getMethod("getDescription").invoke(created));
      assertEquals(1, ((List<?>) structureClass.getMethod("getNodes").invoke(created)).size());

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_cover_node_structure_equals_branches_for_null_and_different_fields()
        throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      var nodeCtor =
          nodeClass.getDeclaredConstructor(
              String.class, String.class, String.class, Integer.class, List.class);
      Object child = nodeCtor.newInstance("Nút con", "#FF6B6B", "star", 0, List.of());
      Object base =
          nodeCtor.newInstance("Nút gốc", "#4A90E2", "book", 1, List.of(child));
      Object same =
          nodeCtor.newInstance("Nút gốc", "#4A90E2", "book", 1, List.of(child));
      Object nullFields = nodeCtor.newInstance(null, null, null, null, null);
      Object nullFieldsSame = nodeCtor.newInstance(null, null, null, null, null);
      Object diffContent =
          nodeCtor.newInstance("Nút khác", "#4A90E2", "book", 1, List.of(child));
      Object diffColor =
          nodeCtor.newInstance("Nút gốc", "#50C878", "book", 1, List.of(child));
      Object diffIcon =
          nodeCtor.newInstance("Nút gốc", "#4A90E2", "target", 1, List.of(child));
      Object diffOrder =
          nodeCtor.newInstance("Nút gốc", "#4A90E2", "book", 2, List.of(child));
      Object diffChildren =
          nodeCtor.newInstance("Nút gốc", "#4A90E2", "book", 1, List.of());

      // ===== ACT & ASSERT =====
      assertEquals(base, same);
      assertEquals(nullFields, nullFieldsSame);
      assertNotEquals(base, diffContent);
      assertNotEquals(base, diffColor);
      assertNotEquals(base, diffIcon);
      assertNotEquals(base, diffOrder);
      assertNotEquals(base, diffChildren);
      assertNotEquals(base, nullFields);
      assertNotEquals(nullFields, base);
      assertNotEquals(base.hashCode(), diffContent.hashCode());
      assertTrue(base.toString().contains("content=Nút gốc"));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }

    @Test
    void it_should_cover_mindmap_structure_equals_branches_for_null_and_different_fields()
        throws Exception {
      // ===== ARRANGE =====
      Class<?> nodeClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$NodeStructure");
      Class<?> structureClass =
          Class.forName("com.fptu.math_master.service.impl.MindmapServiceImpl$MindmapStructure");
      var nodeCtor =
          nodeClass.getDeclaredConstructor(
              String.class, String.class, String.class, Integer.class, List.class);
      var structureCtor =
          structureClass.getDeclaredConstructor(String.class, String.class, List.class);
      Object node = nodeCtor.newInstance("Giải tích", "#4A90E2", "book", 0, List.of());

      Object base = structureCtor.newInstance("Toán 12", "Ôn tập học kỳ", List.of(node));
      Object same = structureCtor.newInstance("Toán 12", "Ôn tập học kỳ", List.of(node));
      Object nullFields = structureCtor.newInstance(null, null, null);
      Object nullFieldsSame = structureCtor.newInstance(null, null, null);
      Object diffTitle = structureCtor.newInstance("Toán 11", "Ôn tập học kỳ", List.of(node));
      Object diffDescription = structureCtor.newInstance("Toán 12", "Luyện đề", List.of(node));
      Object diffNodes = structureCtor.newInstance("Toán 12", "Ôn tập học kỳ", List.of());

      // ===== ACT & ASSERT =====
      assertEquals(base, same);
      assertEquals(nullFields, nullFieldsSame);
      assertNotEquals(base, diffTitle);
      assertNotEquals(base, diffDescription);
      assertNotEquals(base, diffNodes);
      assertNotEquals(base, nullFields);
      assertNotEquals(nullFields, base);
      assertNotEquals(base.hashCode(), diffTitle.hashCode());
      assertTrue(base.toString().contains("title=Toán 12"));

      // ===== VERIFY =====
      verifyNoInteractions(mindmapRepository, mindmapNodeRepository);
    }
  }
}
