package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("MinioUploadServiceImpl - Tests")
class MinioUploadServiceImplTest extends BaseUnitTest {

  @InjectMocks private MinioUploadServiceImpl minioUploadService;

  @Mock private MinioClient minioClient;
  @Mock private MinioProperties minioProperties;

  @Captor private ArgumentCaptor<PutObjectArgs> putObjectArgsCaptor;
  @Captor private ArgumentCaptor<RemoveObjectArgs> removeObjectArgsCaptor;

  private MockMultipartFile buildMultipartFile(
      String name, String fileName, String contentType, String content) {
    return new MockMultipartFile(
        name, fileName, contentType, content.getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  @DisplayName("uploadFile()")
  class UploadFileTests {

    /**
     * Normal case: Upload file thành công vào template bucket mặc định.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>file: lesson-plan.pdf, contentType application/pdf
     *   <li>directory: course-materials
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>ensureBucketExists() -> bucketExists = true (FALSE branch của makeBucket)
     *   <li>extension parsing -> i &gt; 0 (TRUE branch, giữ .pdf)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Trả về object path bắt đầu bằng directory và kết thúc bằng .pdf
     *   <li>{@code minioClient.putObject(...)} được gọi đúng 1 lần
     * </ul>
     */
    @Test
    void it_should_upload_file_to_template_bucket_when_bucket_exists() throws Exception {
      // ===== ARRANGE =====
      MockMultipartFile file =
          buildMultipartFile(
              "material",
              "lesson-plan.pdf",
              "application/pdf",
              "Giáo án Giải tích chương 1");
      when(minioProperties.getTemplateBucket()).thenReturn("slide-templates");
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

      // ===== ACT =====
      String objectPath = minioUploadService.uploadFile(file, "course-materials");

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(objectPath),
          () -> assertTrue(objectPath.startsWith("course-materials/")),
          () -> assertTrue(objectPath.endsWith(".pdf")));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getTemplateBucket();
      verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
      verify(minioClient, times(1)).putObject(putObjectArgsCaptor.capture());
      PutObjectArgs args = putObjectArgsCaptor.getValue();
      assertAll(
          () -> assertEquals("slide-templates", args.bucket()),
          () -> assertEquals(objectPath, args.object()),
          () -> assertEquals("application/pdf", args.contentType()));
      verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Normal case: Upload file thành công với bucket tùy chọn và bucket chưa tồn tại.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>bucketName: teacher-verifications
     *   <li>fileName: profile-document (không có extension)
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>ensureBucketExists() -> bucketExists = false (TRUE branch, gọi makeBucket)
     *   <li>extension parsing -> i &lt;= 0 (FALSE branch, extension rỗng)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Object path được tạo trong directory mà không thêm đuôi extension
     *   <li>{@code makeBucket(...)} được gọi đúng 1 lần
     * </ul>
     */
    @Test
    void it_should_create_bucket_and_upload_file_without_extension_when_bucket_is_missing()
        throws Exception {
      // ===== ARRANGE =====
      MockMultipartFile file =
          buildMultipartFile("verification", "profile-document", "image/png", "profile-content");
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

      // ===== ACT =====
      String objectPath =
          minioUploadService.uploadFile(file, "teacher-profiles", "teacher-verifications");

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(objectPath),
          () -> assertTrue(objectPath.startsWith("teacher-profiles/")),
          () -> assertTrue(!objectPath.contains(".")));

      // ===== VERIFY =====
      verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
      verify(minioClient, times(1)).makeBucket(any(MakeBucketArgs.class));
      verify(minioClient, times(1)).putObject(putObjectArgsCaptor.capture());
      PutObjectArgs args = putObjectArgsCaptor.getValue();
      assertAll(
          () -> assertEquals("teacher-verifications", args.bucket()),
          () -> assertEquals(objectPath, args.object()),
          () -> assertEquals("image/png", args.contentType()));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: MultipartFile có original filename null.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>file.getOriginalFilename(): null
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>Objects.requireNonNull(file.getOriginalFilename()) -> throw NPE branch
     *   <li>Outer catch trong uploadToMinio() wrap thành RuntimeException
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message chứa "Could not upload file to Minio"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_original_filename_is_null() throws Exception {
      // ===== ARRANGE =====
      MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
      when(file.getOriginalFilename()).thenReturn(null);
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> minioUploadService.uploadFile(file, "course-materials", "slide-templates"));
      assertTrue(exception.getMessage().contains("Could not upload file to Minio"));

      // ===== VERIFY =====
      verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
      verify(minioClient, never()).putObject(any(PutObjectArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }
  }

  @Nested
  @DisplayName("uploadFilesAsZip()")
  class UploadFilesAsZipTests {

    /**
     * Normal case: Upload zip thành công và bỏ qua file rỗng.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>files: 1 file có dữ liệu, 1 file rỗng
     *   <li>zipName: teacher-verification-bundle
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>for-loop file.isEmpty() -> TRUE branch (skip) và FALSE branch (thêm vào zip)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Upload thành công vào verification bucket với phần mở rộng .zip
     * </ul>
     */
    @Test
    void it_should_upload_zip_and_skip_empty_files_when_uploading_multiple_files()
        throws Exception {
      // ===== ARRANGE =====
      MockMultipartFile validFile =
          buildMultipartFile("f1", "identity-card.jpg", "image/jpeg", "binary-image-content");
      MockMultipartFile emptyFile = new MockMultipartFile("f2", "empty.txt", "text/plain", new byte[0]);
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

      // ===== ACT =====
      String objectPath =
          minioUploadService.uploadFilesAsZip(
              List.of(validFile, emptyFile), "verification-documents", "teacher-verification-bundle");

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(objectPath),
          () -> assertTrue(objectPath.startsWith("verification-documents/")),
          () -> assertTrue(objectPath.endsWith(".zip")));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getVerificationBucket();
      verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
      verify(minioClient, times(1)).putObject(putObjectArgsCaptor.capture());
      PutObjectArgs args = putObjectArgsCaptor.getValue();
      assertAll(
          () -> assertEquals("teacher-verifications", args.bucket()),
          () -> assertEquals(objectPath, args.object()),
          () -> assertEquals("application/zip", args.contentType()));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: Đọc bytes của một file trong danh sách bị lỗi.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>files: gồm một MultipartFile ném IOException ở getBytes()
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>catch (Exception e) trong uploadFilesAsZip() -> TRUE branch, rethrow RuntimeException
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message "Could not upload zip file to Minio"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_zip_creation_fails() throws Exception {
      // ===== ARRANGE =====
      MultipartFile brokenFile = org.mockito.Mockito.mock(MultipartFile.class);
      when(brokenFile.isEmpty()).thenReturn(false);
      when(brokenFile.getOriginalFilename()).thenReturn("broken-image.jpg");
      when(brokenFile.getBytes()).thenThrow(new IOException("Cannot read file bytes"));
      when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () ->
                  minioUploadService.uploadFilesAsZip(
                      List.of(brokenFile), "verification-documents", "bundle"));
      assertTrue(exception.getMessage().contains("Could not upload zip file to Minio"));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getVerificationBucket();
      verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
      verify(minioClient, never()).putObject(any(PutObjectArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }
  }

  @Nested
  @DisplayName("Presigned URL and download methods")
  class PresignedAndDownloadTests {

    /**
     * Normal case: Tạo presigned URL thành công với key là full URL.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>key: https://cdn.example.com/slide-templates/math/chapter-1.pdf
     *   <li>publicEndpoint: http://localhost:9000/minio (có path cần strip)
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>normalizeObjectKey() -> nhánh xử lý URL + bỏ prefix bucket
     *   <li>buildPresigner() -> nhánh strip path khỏi publicEndpoint
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>URL trả về không null và chứa normalized object key
     * </ul>
     */
    @Test
    void it_should_generate_presigned_url_when_book_key_has_redundant_bucket_prefix() {
      when(minioProperties.getPublicEndpoint()).thenReturn("");
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minio-admin");
      when(minioProperties.getSecretKey()).thenReturn("minio-secret");

      String url =
          minioUploadService.getPresignedUrl(
              "slide-templates/slide-templates/books/pdfs/chapter.pdf", "slide-templates");

      assertAll(
          () -> assertNotNull(url),
          () -> assertTrue(url.contains("books/pdfs/chapter.pdf")),
          () -> assertTrue(url.contains("X-Amz-Signature")));

      verify(minioProperties, times(1)).getPublicEndpoint();
      verify(minioProperties, times(1)).getEndpoint();
      verify(minioProperties, times(1)).getAccessKey();
      verify(minioProperties, times(1)).getSecretKey();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    @Test
    void it_should_generate_presigned_url_when_key_is_full_url() {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("http://localhost:9000/minio");
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minio-admin");
      when(minioProperties.getSecretKey()).thenReturn("minio-secret");

      // ===== ACT =====
      String url =
          minioUploadService.getPresignedUrl(
              "https://cdn.example.com/slide-templates/math/chapter-1.pdf", "slide-templates");

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(url),
          () -> assertTrue(url.contains("chapter-1.pdf")),
          () -> assertTrue(url.contains("X-Amz-Signature")));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getPublicEndpoint();
      verify(minioProperties, never()).getEndpoint();
      verify(minioProperties, times(1)).getAccessKey();
      verify(minioProperties, times(1)).getSecretKey();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Normal case: Tạo presigned download URL với filename tùy chỉnh.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>fileName: "Bai giang Chuong 1.pdf" (có khoảng trắng cần encode)
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>safeFileName branch -> dùng filename truyền vào (không fallback "download")
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>URL chứa content-disposition đã encode filename
     * </ul>
     */
    @Test
    void it_should_generate_presigned_download_url_with_encoded_file_name() {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("");
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minio-admin");
      when(minioProperties.getSecretKey()).thenReturn("minio-secret");

      // ===== ACT =====
      String url =
          minioUploadService.getPresignedDownloadUrl(
              "/slide-templates/materials/lesson-1.pdf",
              "slide-templates",
              "Bai giang Chuong 1.pdf");

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(url),
          () -> assertTrue(url.contains("lesson-1.pdf")),
          () -> assertTrue(url.contains("content-disposition")));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getPublicEndpoint();
      verify(minioProperties, times(1)).getEndpoint();
      verify(minioProperties, times(1)).getAccessKey();
      verify(minioProperties, times(1)).getSecretKey();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Normal case: Presigned download URL dùng filename fallback khi filename null.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>fileName: null
     *   <li>publicEndpoint: có path "/" nên không strip path
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>safeFileName branch -> dùng fallback "download"
     *   <li>buildPresigner() path-check -> FALSE branch với path "/"
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>URL được tạo thành công và chứa query content-disposition
     * </ul>
     */
    @Test
    void it_should_generate_presigned_download_url_with_default_filename_when_filename_is_null() {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("http://localhost:9000/");
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minio-admin");
      when(minioProperties.getSecretKey()).thenReturn("minio-secret");

      // ===== ACT =====
      String url =
          minioUploadService.getPresignedDownloadUrl(
              "materials/lesson-2.pdf", "slide-templates", null);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(url),
          () -> assertTrue(url.contains("lesson-2.pdf")),
          () -> assertTrue(url.contains("content-disposition")));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getPublicEndpoint();
      verify(minioProperties, never()).getEndpoint();
      verify(minioProperties, times(1)).getAccessKey();
      verify(minioProperties, times(1)).getSecretKey();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: Key dạng URL không hợp lệ làm URI parsing thất bại và presign lỗi.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>key: "https://%" (URI invalid)
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>normalizeObjectKey() URL parse catch branch
     *   <li>getPresignedUrl() catch branch -> throw RuntimeException
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link RuntimeException} với message "Could not generate download URL"
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_presigned_url_generation_uses_invalid_endpoint() {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("://invalid-endpoint");
      when(minioProperties.getAccessKey()).thenReturn("minio-admin");
      when(minioProperties.getSecretKey()).thenReturn("minio-secret");

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> minioUploadService.getPresignedUrl("materials/chapter-4.pdf", "slide-templates"));
      assertTrue(exception.getMessage().contains("Could not generate download URL"));

      // ===== VERIFY =====
      verify(minioProperties, times(1)).getPublicEndpoint();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: Download file thất bại ở Minio client.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>key: "slide-templates/materials/not-found.pdf"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>downloadFile() catch branch -> map về DOCUMENT_NOT_FOUND
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code DOCUMENT_NOT_FOUND}
     * </ul>
     */
    @Test
    void it_should_throw_document_not_found_when_download_from_minio_fails() throws Exception {
      // ===== ARRANGE =====
      when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("No object"));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () ->
                  minioUploadService.downloadFile(
                      "slide-templates/materials/not-found.pdf", "slide-templates"));
      assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Normal case: Download file thành công sau khi normalize key.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>key: "/slide-templates/materials/algebra.pdf"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>normalizeObjectKey() -> remove leading slash và bucket prefix
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Byte array trả về đúng với dữ liệu Minio response
     * </ul>
     */
    @Test
    void it_should_return_file_bytes_when_download_succeeds() throws Exception {
      // ===== ARRANGE =====
      byte[] expected = "algebra-content".getBytes(StandardCharsets.UTF_8);
      GetObjectResponse response = org.mockito.Mockito.mock(GetObjectResponse.class);
      when(response.readAllBytes()).thenReturn(expected);
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

      // ===== ACT =====
      byte[] actual =
          minioUploadService.downloadFile("/slide-templates/materials/algebra.pdf", "slide-templates");

      // ===== ASSERT =====
      assertArrayEquals(expected, actual);

      // ===== VERIFY =====
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verify(response, times(1)).readAllBytes();
      verify(response, times(1)).close();
      verifyNoMoreInteractions(minioClient, minioProperties);
    }
  }

  @Nested
  @DisplayName("deleteFile()")
  class DeleteFileTests {

    /**
     * Normal case: deleteFile(filePath) dùng template bucket mặc định.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>filePath: "/slide-templates/materials/chapter-2.pdf"
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>deleteFile(String) -> delegate sang deleteFile(String, bucketName)
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>{@code removeObject(...)} nhận key đã normalize bỏ bucket prefix
     * </ul>
     */
    @Test
    void it_should_delete_file_using_template_bucket_when_bucket_is_not_provided() throws Exception {
      // ===== ARRANGE =====
      when(minioProperties.getTemplateBucket()).thenReturn("slide-templates");

      // ===== ACT =====
      minioUploadService.deleteFile("/slide-templates/materials/chapter-2.pdf");

      // ===== ASSERT =====
      // ===== VERIFY =====
      verify(minioProperties, times(1)).getTemplateBucket();
      verify(minioClient, times(1)).removeObject(removeObjectArgsCaptor.capture());
      RemoveObjectArgs args = removeObjectArgsCaptor.getValue();
      assertAll(
          () -> assertEquals("slide-templates", args.bucket()),
          () -> assertEquals("materials/chapter-2.pdf", args.object()));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: Minio ném lỗi khi xóa file.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>filePath: "materials/chapter-3.pdf"
     *   <li>bucketName: slide-templates
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>catch branch trong deleteFile(String, String) -> log lỗi và không throw
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Method không ném exception dù removeObject lỗi
     * </ul>
     */
    @Test
    void it_should_swallow_exception_when_delete_file_fails() throws Exception {
      // ===== ARRANGE =====
      doThrow(new RuntimeException("Delete failed"))
          .when(minioClient)
          .removeObject(argThat(args -> "slide-templates".equals(args.bucket())));

      // ===== ACT =====
      minioUploadService.deleteFile("materials/chapter-3.pdf", "slide-templates");

      // ===== ASSERT =====
      // ===== VERIFY =====
      verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }

    /**
     * Abnormal case: deleteFile nhận key null.
     *
     * <p>Input:
     *
     * <ul>
     *   <li>filePath: null
     *   <li>bucketName: slide-templates
     * </ul>
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>normalizeObjectKey() -> key == null (TRUE branch)
     *   <li>deleteFile catch branch khi removeObject nhận object null và ném lỗi
     * </ul>
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Method không ném exception ra ngoài
     * </ul>
     */
    @Test
    void it_should_not_throw_when_delete_file_receives_null_key() throws Exception {
      // ===== ARRANGE =====
      // ===== ACT =====
      minioUploadService.deleteFile(null, "slide-templates");

      // ===== ASSERT =====
      // ===== VERIFY =====
      verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
      verifyNoMoreInteractions(minioClient, minioProperties);
    }
  }
}
