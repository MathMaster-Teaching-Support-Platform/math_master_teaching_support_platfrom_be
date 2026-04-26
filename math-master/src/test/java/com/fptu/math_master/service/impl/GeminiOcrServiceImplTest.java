package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.service.GeminiService;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@DisplayName("GeminiOcrServiceImpl - Tests")
class GeminiOcrServiceImplTest extends BaseUnitTest {

  @InjectMocks private GeminiOcrServiceImpl geminiOcrService;

  @Mock private TeacherProfileRepository teacherProfileRepository;
  @Mock private MinioClient minioClient;
  @Mock private MinioProperties minioProperties;
  @Mock private GeminiService geminiService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  private TeacherProfile buildTeacherProfile(
      String fullName, String position, String schoolName, String documentKey, String documentPath) {
    TeacherProfile profile = new TeacherProfile();
    profile.setFullName(fullName);
    profile.setPosition(position);
    profile.setSchoolName(schoolName);
    profile.setVerificationDocumentKey(documentKey);
    profile.setVerificationDocumentPath(documentPath);
    return profile;
  }

  private byte[] buildZipBytesWithEntry(String entryName, byte[] entryContent) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
      ZipEntry entry = new ZipEntry(entryName);
      zipOut.putNextEntry(entry);
      zipOut.write(entryContent);
      zipOut.closeEntry();
    }
    return out.toByteArray();
  }

  private GetObjectResponse buildGetObjectResponse(byte[] data) throws IOException {
    GetObjectResponse response = Mockito.mock(GetObjectResponse.class);
    when(response.readAllBytes()).thenReturn(data);
    return response;
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = GeminiOcrServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(geminiOcrService, args);
  }

  @Nested
  @DisplayName("processProfileVerification()")
  class ProcessProfileVerificationTests {

    /**
     * Abnormal case: Hồ sơ giáo viên không tồn tại.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>profileId: UUID hợp lệ nhưng repository không tìm thấy dữ liệu
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>findById(profileId) -> Optional.empty (nhánh throw PROFILE_NOT_FOUND)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code PROFILE_NOT_FOUND}
     * </ul>
     */
    @Test
    void it_should_throw_profile_not_found_when_profile_does_not_exist() {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> geminiOcrService.processProfileVerification(profileId));
      assertEquals(ErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: Cả verificationDocumentKey và verificationDocumentPath đều null.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>profile: có đủ thông tin cơ bản nhưng không có key/path tài liệu xác minh
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>resolveDocumentObjectKeys() -> keys.isEmpty() (TRUE branch, throw DOCUMENT_NOT_FOUND)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code DOCUMENT_NOT_FOUND}
     * </ul>
     */
    @Test
    void it_should_throw_document_not_found_when_profile_has_no_document_key_and_path() {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Nguyen Van Minh", "Giáo viên Toán", "Trường THPT Lê Quý Đôn", null, null);

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> geminiOcrService.processProfileVerification(profileId));
      assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Normal case: Tài liệu là ảnh và dữ liệu OCR khớp đầy đủ 3 trường bắt buộc.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>documentKey: "profiles/teacher-card.png"
     *   <li>geminiResponse: JSON hợp lệ, fullName/schoolName khớp và position là giáo viên Toán
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>isZipFile() -> FALSE branch (xử lý ảnh trực tiếp)
     *   <li>compareData() -> allFieldsPass = true (nhánh summary thành công)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về kết quả match = true, score = 100, summary chứa "THÀNH CÔNG"
     * </ul>
     */
    @Test
    void it_should_return_match_result_when_image_data_matches_profile() throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Nguyen Van Minh",
              "Giáo viên Toán",
              "Trường THPT Lê Quý Đôn",
              "profiles/teacher-card.png",
              null);
      byte[] imageBytes = "png-binary".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse objectResponse = buildGetObjectResponse(imageBytes);
      String geminiResponse =
          "{\"fullName\":\"Nguyen Van Minh\",\"position\":\"Giáo viên Toán\",\"schoolName\":\"Trường THPT Lê Quý Đôn\"}";

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn(geminiResponse);

      // ===== ACT =====
      OcrComparisonResult result = geminiOcrService.processProfileVerification(profileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(Boolean.TRUE, result.getIsMatch()),
          () -> assertEquals(100.0, result.getMatchScore()),
          () -> assertTrue(result.getSummary().contains("THÀNH CÔNG")),
          () -> assertEquals(3, result.getFieldComparisons().size()));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Normal case: key đầu tiên đọc MinIO thất bại, service fallback sang key thứ hai thành công.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>verificationDocumentKey: URL đầy đủ trỏ vào bucket
     *   <li>verificationDocumentPath: object key hiện tại
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>normalizeObjectKey() -> URL branch + bucket-prefix strip branch
     *   <li>for-loop candidate keys -> lần 1 exception, lần 2 thành công
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Service vẫn xử lý thành công và gọi MinIO 2 lần
     * </ul>
     */
    @Test
    void it_should_fallback_to_secondary_document_key_when_primary_key_read_fails()
        throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Tran Thi Mai",
              "Giảng viên Khoa Toán",
              "Đại học Sư phạm Hà Nội",
              "https://minio.local/teacher-verifications/folder/primary-fail.jpg",
              "folder/secondary-success.jpg");
      byte[] imageBytes = "secondary-image".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse objectResponse = buildGetObjectResponse(imageBytes);
      String geminiResponse =
          "{\"fullName\":\"Tran Thi Mai\",\"position\":\"Giảng viên Toán\",\"schoolName\":\"Đại học Sư phạm Hà Nội\"}";

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class)))
          .thenThrow(new RuntimeException("primary key missing"))
          .thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn(geminiResponse);

      // ===== ACT =====
      OcrComparisonResult result = geminiOcrService.processProfileVerification(profileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(Boolean.TRUE, result.getIsMatch()),
          () -> assertEquals(100.0, result.getMatchScore()));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(2)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: Tất cả candidate key đều không thể đọc từ MinIO.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>profile có cả key và path, nhưng MinIO trả lỗi cho mọi lần đọc
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>for-loop candidate keys -> tất cả lần đọc đều exception
     *   <li>fileBytes == null (TRUE branch, throw DOCUMENT_NOT_FOUND)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code DOCUMENT_NOT_FOUND}
     * </ul>
     */
    @Test
    void it_should_throw_document_not_found_when_all_candidate_keys_fail_to_read_from_minio()
        throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Le Minh Chau",
              "Giáo viên Toán",
              "Trường THCS Nguyễn Du",
              "primary/fail-1.png",
              "secondary/fail-2.png");

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class)))
          .thenThrow(new RuntimeException("read fail #1"))
          .thenThrow(new RuntimeException("read fail #2"));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> geminiOcrService.processProfileVerification(profileId));
      assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(2)).getObject(any(GetObjectArgs.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Normal case: Tài liệu ZIP có chứa ảnh hợp lệ để OCR.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>documentPath: "documents/teacher-evidence.zip"
     *   <li>zip bytes chứa một entry ảnh "teacher-card.JPG"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>isZipFile() -> TRUE branch
     *   <li>extractDataFromZipBytes() -> tìm thấy entry ảnh và return sớm
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về kết quả match true và parse thành công dữ liệu OCR từ ảnh trong ZIP
     * </ul>
     */
    @Test
    void it_should_process_zip_file_when_zip_contains_supported_image_entry() throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Pham Huu Tin",
              "Giáo viên Toán",
              "Trường THPT Chuyên Lam Sơn",
              null,
              "documents/teacher-evidence.zip");
      byte[] zipBytes = buildZipBytesWithEntry("teacher-card.JPG", "image-in-zip".getBytes());
      GetObjectResponse objectResponse = buildGetObjectResponse(zipBytes);
      String geminiResponse =
          "{\"fullName\":\"Pham Huu Tin\",\"position\":\"Giáo viên Toán\",\"schoolName\":\"Trường THPT Chuyên Lam Sơn\"}";

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn(geminiResponse);

      // ===== ACT =====
      OcrComparisonResult result = geminiOcrService.processProfileVerification(profileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(Boolean.TRUE, result.getIsMatch()),
          () -> assertEquals(100.0, result.getMatchScore()));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: ZIP không chứa ảnh hợp lệ.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>zip bytes chỉ chứa file .txt
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>extractDataFromZipBytes() -> không tìm thấy ảnh, throw "No image files found in ZIP"
     *   <li>extractDataFromFileBytes() -> catch exception và wrap "OCR extraction failed"
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "OCR extraction failed"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_zip_does_not_contain_image_entries() throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Doan Thi Hoa",
              "Giáo viên Toán",
              "Trường THCS Trưng Vương",
              "proof/no-image.zip",
              null);
      byte[] zipBytes =
          buildZipBytesWithEntry("metadata/readme.txt", "this zip has no image".getBytes());
      GetObjectResponse objectResponse = buildGetObjectResponse(zipBytes);

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> geminiOcrService.processProfileVerification(profileId));
      assertTrue(exception.getMessage().contains("OCR extraction failed"));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: Gemini trả về JSON không hợp lệ nên parse thất bại.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>geminiResponse: "invalid-json"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>parseGeminiResponse() -> catch parse exception branch
     *   <li>extractDataFromImageBytes() -> catch and wrap "Gemini OCR failed"
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Gemini OCR failed"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_gemini_response_is_invalid_json() throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Truong Quang Huy",
              "Giáo viên Toán",
              "Trường THPT Chu Văn An",
              "images/ocr-source.png",
              null);
      byte[] imageBytes = "plain-image".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse objectResponse = buildGetObjectResponse(imageBytes);

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn("invalid-json");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class, () -> geminiOcrService.processProfileVerification(profileId));
      assertTrue(exception.getMessage().contains("Gemini OCR failed"));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: OCR trả thiếu nhiều field bắt buộc.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>geminiResponse có fullName = null, position = null và schoolName = null
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>compareField() -> branch ocrValue == null || profileValue == null
     *   <li>validateTeacherPosition() -> position null branch
     *   <li>compareData() -> missingFields không rỗng (nhánh failureReason thiếu dữ liệu)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Kết quả mismatch, score = 0, summary chứa "thiếu:"
     * </ul>
     */
    @Test
    void it_should_return_missing_fields_failure_summary_when_ocr_misses_mandatory_data()
        throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Ngo Duc Khang",
              "Giáo viên Toán",
              "Trường THCS Lương Thế Vinh",
              "images/missing-data.png",
              null);
      byte[] imageBytes = "img".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse objectResponse = buildGetObjectResponse(imageBytes);
      String geminiResponse = "{\"fullName\":null,\"position\":null,\"schoolName\":null}";

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn(geminiResponse);

      // ===== ACT =====
      OcrComparisonResult result = geminiOcrService.processProfileVerification(profileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(Boolean.FALSE, result.getIsMatch()),
          () -> assertEquals(0.0, result.getMatchScore()),
          () -> assertTrue(result.getSummary().contains("thiếu:")));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    /**
     * Abnormal case: OCR có đủ dữ liệu nhưng không khớp hồ sơ và position không phải chuyên môn Toán.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>position OCR: "Giáo viên Văn"
     *   <li>fullName và schoolName OCR khác với profile
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>validateTeacherPosition() -> !hasMathSubject branch
     *   <li>compareData() -> missingFields rỗng, mismatchedFields không rỗng
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Kết quả mismatch và summary chứa "không khớp:"
     * </ul>
     */
    @Test
    void it_should_return_mismatch_failure_summary_when_all_fields_present_but_values_do_not_match()
        throws Exception {
      // ===== ARRANGE =====
      UUID profileId = UUID.randomUUID();
      TeacherProfile profile =
          buildTeacherProfile(
              "Vo Thanh Tam", "Giáo viên Toán", "Trường THPT Hùng Vương", "images/mismatch.png", null);
      byte[] imageBytes = "img-mismatch".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse objectResponse = buildGetObjectResponse(imageBytes);
      String geminiResponse =
          "{\"fullName\":\"Le Van Son\",\"position\":\"Giáo viên Văn\",\"schoolName\":\"Trường THPT Quang Trung\"}";

      when(teacherProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);
      when(geminiService.analyzeImageWithPrompt(any(byte[].class), any(String.class)))
          .thenReturn(geminiResponse);

      // ===== ACT =====
      OcrComparisonResult result = geminiOcrService.processProfileVerification(profileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(Boolean.FALSE, result.getIsMatch()),
          () -> assertTrue(result.getMatchScore() < 50.0),
          () -> assertTrue(result.getSummary().contains("không khớp:")));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).findById(profileId);
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(geminiService, times(1)).analyzeImageWithPrompt(any(byte[].class), any(String.class));
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }
  }

  @Nested
  @DisplayName("Private branch methods")
  class PrivateBranchMethodTests {

    @Test
    void it_should_reject_position_when_role_is_staff_without_math_subject() throws Exception {
      // ===== ARRANGE =====
      String position = "Nhân viên hành chính";

      // ===== ACT =====
      OcrComparisonResult.FieldComparison result =
          invokePrivateMethod(
              "validateTeacherPosition",
              new Class<?>[] {String.class},
              position);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Boolean.FALSE, result.getMatches()),
          () -> assertTrue(result.getNotes().contains("TỪ CHỐI")));

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_fail_position_when_math_exists_but_missing_teaching_role_keywords()
        throws Exception {
      // ===== ARRANGE =====
      String position = "Tổ trưởng Tổ Toán";

      // ===== ACT =====
      OcrComparisonResult.FieldComparison result =
          invokePrivateMethod(
              "validateTeacherPosition",
              new Class<?>[] {String.class},
              position);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Boolean.FALSE, result.getMatches()),
          () -> assertTrue(result.getNotes().contains("Thiếu chức danh giảng dạy")));

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_accept_position_when_ascii_teaching_and_math_keywords_are_present()
        throws Exception {
      // ===== ARRANGE =====
      String position = "giao vien toan";

      // ===== ACT =====
      OcrComparisonResult.FieldComparison result =
          invokePrivateMethod(
              "validateTeacherPosition",
              new Class<?>[] {String.class},
              position);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Boolean.TRUE, result.getMatches()),
          () -> assertEquals(100.0, result.getSimilarity()),
          () -> assertTrue(result.getNotes().contains("HỢP LỆ")));

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_mark_field_as_match_when_both_ocr_and_profile_values_are_null()
        throws Exception {
      // ===== ARRANGE =====
      String fieldName = "Họ và tên";

      // ===== ACT =====
      OcrComparisonResult.FieldComparison result =
          invokePrivateMethod(
              "compareField",
              new Class<?>[] {String.class, String.class, String.class},
              fieldName,
              null,
              null);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(Boolean.TRUE, result.getMatches()),
          () -> assertEquals(100.0, result.getSimilarity()),
          () -> assertEquals("Both values are null", result.getNotes()));

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_keep_original_value_when_normalize_object_key_receives_invalid_url()
        throws Exception {
      // ===== ARRANGE =====
      String invalidUrl = "https://%zz-bad-url";

      // ===== ACT =====
      String result =
          invokePrivateMethod(
              "normalizeObjectKey",
              new Class<?>[] {String.class, String.class},
              invalidUrl,
              "teacher-verifications");

      // ===== ASSERT =====
      assertEquals("https://%zz-bad-url", result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_return_null_for_missing_json_field_when_parsing_gemini_response() throws Exception {
      // ===== ARRANGE =====
      String response = "{\"fullName\":\"Nguyen Van A\",\"position\":\"Giáo viên Toán\"}";

      // ===== ACT =====
      OcrComparisonResult.OcrExtractedData result =
          invokePrivateMethod(
              "parseGeminiResponse",
              new Class<?>[] {String.class},
              response);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Nguyen Van A", result.getFullName()),
          () -> assertEquals("Giáo viên Toán", result.getPosition()),
          () -> assertEquals(null, result.getSchoolName()));

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }

    @Test
    void it_should_return_empty_string_when_normalize_string_receives_null_input() throws Exception {
      // ===== ARRANGE =====
      String rawValue = null;

      // ===== ACT =====
      String normalized =
          invokePrivateMethod(
              "normalizeString",
              new Class<?>[] {String.class},
              rawValue);

      // ===== ASSERT =====
      assertEquals("", normalized);

      // ===== VERIFY =====
      verifyNoMoreInteractions(teacherProfileRepository, minioClient, geminiService);
    }
  }
}
