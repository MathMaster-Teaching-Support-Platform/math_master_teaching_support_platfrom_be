package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.entity.TeachingResource;
import com.fptu.math_master.enums.TeachingResourceType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeachingResourceRepository;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("ResourceServiceImpl - Tests")
class ResourceServiceImplTest extends BaseUnitTest {

  @InjectMocks private ResourceServiceImpl resourceService;

  @Mock private UploadService uploadService;
  @Mock private TeachingResourceRepository teachingResourceRepository;
  @Mock private MultipartFile file;

  private static final UUID USER_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
  private static final UUID RESOURCE_ID = UUID.fromString("80000000-0000-0000-0000-000000000002");

  private TeachingResource buildResource(UUID id, UUID createdBy) {
    TeachingResource tr = new TeachingResource();
    tr.setId(id);
    tr.setName("Mindmap dai so");
    tr.setType(TeachingResourceType.MINDMAP);
    tr.setFileUrl("https://cdn.example.com/resource.xmind");
    tr.setCreatedBy(createdBy);
    tr.setCreatedAt(Instant.parse("2026-04-26T05:00:00Z"));
    return tr;
  }

  @Nested
  @DisplayName("uploadResource()")
  class UploadResourceTests {

    @Test
    void it_should_upload_resource_when_user_is_teacher_and_input_is_valid() {
      // ===== ARRANGE =====
      TeachingResource saved = buildResource(RESOURCE_ID, USER_ID);
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("mindmap.xmind");
        when(uploadService.uploadFile(file, "teaching-resources/mindmap"))
            .thenReturn("https://cdn.example.com/resource.xmind");
        when(teachingResourceRepository.save(any(TeachingResource.class))).thenReturn(saved);

        // ===== ACT =====
        var response = resourceService.uploadResource("  Mindmap dai so  ", TeachingResourceType.MINDMAP, file);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(response),
            () -> assertEquals(RESOURCE_ID, response.getId()),
            () -> assertEquals("Mindmap dai so", response.getName()),
            () -> assertEquals(TeachingResourceType.MINDMAP, response.getType()));
      }
    }

    @Test
    void it_should_throw_unauthorized_when_user_has_neither_teacher_nor_admin_role() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Slide", TeachingResourceType.SLIDE, file));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_invalid_key_when_upload_name_is_blank() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("   ", TeachingResourceType.PDF, file));
        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_invalid_key_when_type_is_null() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(AppException.class, () -> resourceService.uploadResource("Doc", null, file));
        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_invalid_key_when_file_is_null_or_empty() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(true);

        // ===== ACT & ASSERT =====
        AppException exceptionForNullFile =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Resource", TeachingResourceType.DOC, null));
        AppException exceptionForEmptyFile =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Resource", TeachingResourceType.DOC, file));
        assertAll(
            () -> assertEquals(ErrorCode.INVALID_KEY, exceptionForNullFile.getErrorCode()),
            () -> assertEquals(ErrorCode.INVALID_KEY, exceptionForEmptyFile.getErrorCode()));
      }
    }

    @Test
    void it_should_throw_file_too_large_when_size_exceeds_limit() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(ResourceServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        when(file.getOriginalFilename()).thenReturn("huge.pdf");

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Large PDF", TeachingResourceType.PDF, file));
        assertEquals(ErrorCode.RESOURCE_FILE_TOO_LARGE, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_invalid_file_format_when_file_has_no_extension() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("invalidfile");

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Invalid", TeachingResourceType.PDF, file));
        assertEquals(ErrorCode.INVALID_FILE_FORMAT, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_invalid_file_format_when_extension_not_allowed_for_type() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("notes.pdf");

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> resourceService.uploadResource("Wrong extension", TeachingResourceType.SLIDE, file));
        assertEquals(ErrorCode.INVALID_FILE_FORMAT, exception.getErrorCode());
      }
    }

    @Test
    void it_should_accept_allowed_extensions_for_each_resource_type() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(uploadService.uploadFile(eq(file), any())).thenReturn("https://cdn.example.com/file");
        when(teachingResourceRepository.save(any(TeachingResource.class)))
            .thenAnswer(
                inv -> {
                  TeachingResource tr = inv.getArgument(0);
                  tr.setId(UUID.randomUUID());
                  return tr;
                });

        when(file.getOriginalFilename()).thenReturn("slides.pptx");
        resourceService.uploadResource("Slide", TeachingResourceType.SLIDE, file);

        when(file.getOriginalFilename()).thenReturn("mindmap.svg");
        resourceService.uploadResource("Mindmap", TeachingResourceType.MINDMAP, file);

        when(file.getOriginalFilename()).thenReturn("notes.docx");
        resourceService.uploadResource("Doc", TeachingResourceType.DOC, file);

        when(file.getOriginalFilename()).thenReturn("ebook.pdf");
        resourceService.uploadResource("Pdf", TeachingResourceType.PDF, file);

        when(file.getOriginalFilename()).thenReturn("image.webp");
        resourceService.uploadResource("Image", TeachingResourceType.IMAGE, file);

        // ===== VERIFY =====
        verify(teachingResourceRepository, times(5)).save(any(TeachingResource.class));
      }
    }
  }

  @Nested
  @DisplayName("getResource() and deleteResource()")
  class ReadAndDeleteTests {

    @Test
    void it_should_return_resource_when_id_exists_and_not_deleted() {
      // ===== ARRANGE =====
      TeachingResource resource = buildResource(RESOURCE_ID, USER_ID);
      when(teachingResourceRepository.findByIdAndNotDeleted(RESOURCE_ID)).thenReturn(Optional.of(resource));

      // ===== ACT =====
      var response = resourceService.getResource(RESOURCE_ID);

      // ===== ASSERT =====
      assertEquals(RESOURCE_ID, response.getId());
    }

    @Test
    void it_should_throw_not_found_when_get_resource_with_unknown_id() {
      // ===== ARRANGE =====
      when(teachingResourceRepository.findByIdAndNotDeleted(RESOURCE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> resourceService.getResource(RESOURCE_ID));
      assertEquals(ErrorCode.TEACHING_RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_soft_delete_resource_when_user_is_owner() {
      // ===== ARRANGE =====
      TeachingResource resource = buildResource(RESOURCE_ID, USER_ID);
      when(teachingResourceRepository.findByIdAndNotDeleted(RESOURCE_ID)).thenReturn(Optional.of(resource));
      when(teachingResourceRepository.save(resource)).thenReturn(resource);
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT =====
        resourceService.deleteResource(RESOURCE_ID);
      }

      // ===== ASSERT =====
      assertNotNull(resource.getDeletedAt());
      assertEquals(USER_ID, resource.getDeletedBy());
      verify(uploadService, times(1)).deleteFile(resource.getFileUrl());
      verify(teachingResourceRepository, times(1)).save(resource);
    }

    @Test
    void it_should_soft_delete_resource_when_user_is_admin_but_not_owner() {
      // ===== ARRANGE =====
      TeachingResource resource = buildResource(RESOURCE_ID, UUID.fromString("80000000-0000-0000-0000-000000000099"));
      when(teachingResourceRepository.findByIdAndNotDeleted(RESOURCE_ID)).thenReturn(Optional.of(resource));
      when(teachingResourceRepository.save(resource)).thenReturn(resource);
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(true);

        // ===== ACT =====
        resourceService.deleteResource(RESOURCE_ID);
      }

      // ===== ASSERT =====
      assertNotNull(resource.getDeletedAt());
      verify(uploadService, times(1)).deleteFile(resource.getFileUrl());
    }

    @Test
    void it_should_throw_access_denied_when_user_is_not_admin_and_not_owner() {
      // ===== ARRANGE =====
      TeachingResource resource = buildResource(RESOURCE_ID, UUID.fromString("80000000-0000-0000-0000-000000000088"));
      when(teachingResourceRepository.findByIdAndNotDeleted(RESOURCE_ID)).thenReturn(Optional.of(resource));
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(AppException.class, () -> resourceService.deleteResource(RESOURCE_ID));
        assertEquals(ErrorCode.TEACHING_RESOURCE_ACCESS_DENIED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(teachingResourceRepository, times(1)).findByIdAndNotDeleted(RESOURCE_ID);
      verify(uploadService, never()).deleteFile(any());
      verify(teachingResourceRepository, never()).save(any(TeachingResource.class));
      verifyNoMoreInteractions(uploadService, teachingResourceRepository);
    }
  }
}
