package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.UploadService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioUploadServiceImpl implements UploadService {

  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  @Override
  public String uploadFile(MultipartFile file, String directory) {
    String bucketName = minioProperties.getTemplateBucket();
    return uploadToMinio(file, directory, bucketName);
  }

  @Override
  public String uploadFile(MultipartFile file, String directory, String bucketName) {
    return uploadToMinio(file, directory, bucketName);
  }

  @Override
  public String uploadFilesAsZip(List<MultipartFile> files, String directory, String zipName) {
    String bucketName = minioProperties.getVerificationBucket();

    try {
      ensureBucketExists(bucketName);

      // Create Zip archive in memory
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        for (MultipartFile file : files) {
          if (file.isEmpty()) continue;

          String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
          ZipEntry entry = new ZipEntry(fileName);
          zos.putNextEntry(entry);
          zos.write(file.getBytes());
          zos.closeEntry();
        }
      }

      byte[] zipBytes = baos.toByteArray();
      String finalZipName = zipName + "_" + UUID.randomUUID().toString() + ".zip";
      String objectName = directory + "/" + finalZipName;

      // Upload Zip file
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(new ByteArrayInputStream(zipBytes), zipBytes.length, -1)
              .contentType("application/zip")
              .build());

      log.info("Zip file uploaded successfully to Minio: {}/{}", bucketName, objectName);
      return objectName;
    } catch (Exception e) {
      log.error("Error uploading zip file to Minio", e);
      throw new RuntimeException("Could not upload zip file to Minio", e);
    }
  }

  private String uploadToMinio(MultipartFile file, String directory, String bucketName) {
    try {
      ensureBucketExists(bucketName);

      // Normalize file name
      String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
      String extension = "";
      int i = originalFileName.lastIndexOf('.');
      if (i > 0) {
        extension = originalFileName.substring(i);
      }

      // Generate unique file name
      String fileName = UUID.randomUUID().toString() + extension;
      String objectName = directory + "/" + fileName;

      // Upload file
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(file.getInputStream(), file.getSize(), -1)
              .contentType(file.getContentType())
              .build());

      log.info("File uploaded successfully to Minio: {}/{}", bucketName, objectName);

      // Return the object path (bucket/directory/filename) or just directory/filename
      // For now, let's return the full path that can be used to construct a link or used in delete
      return objectName;
    } catch (Exception e) {
      log.error("Error uploading file to Minio", e);
      throw new RuntimeException("Could not upload file to Minio", e);
    }
  }

  @Override
  public String getPresignedUrl(String key, String bucketName) {
    String normalizedKey = normalizeObjectKey(key, bucketName);
    try (S3Presigner presigner = buildPresigner()) {
      PresignedGetObjectRequest presigned =
          presigner.presignGetObject(
              GetObjectPresignRequest.builder()
                  .signatureDuration(Duration.ofHours(1))
                  .getObjectRequest(
                      GetObjectRequest.builder()
                          .bucket(bucketName)
                          .key(normalizedKey)
                          .build())
                  .build());
      return presigned.url().toString();
    } catch (Exception e) {
      log.error("Error generating pre-signed URL for Minio: {}/{}", bucketName, normalizedKey, e);
      throw new RuntimeException("Could not generate download URL", e);
    }
  }

  @Override
  public String getPresignedDownloadUrl(String key, String bucketName, String fileName) {
    String normalizedKey = normalizeObjectKey(key, bucketName);
    String safeFileName = (fileName != null && !fileName.isBlank()) ? fileName : "download";
    // Percent-encode the filename so it is safe for an HTTP header value.
    String encodedName;
    try {
      encodedName = java.net.URLEncoder.encode(safeFileName, java.nio.charset.StandardCharsets.UTF_8)
          .replace("+", "%20");
    } catch (Exception e) {
      encodedName = safeFileName.replaceAll("[^\\w._-]", "_");
    }
    String contentDisposition = "attachment; filename*=UTF-8''" + encodedName;
    try (S3Presigner presigner = buildPresigner()) {
      PresignedGetObjectRequest presigned =
          presigner.presignGetObject(
              GetObjectPresignRequest.builder()
                  .signatureDuration(Duration.ofHours(1))
                  .getObjectRequest(
                      GetObjectRequest.builder()
                          .bucket(bucketName)
                          .key(normalizedKey)
                          .responseContentDisposition(contentDisposition)
                          .build())
                  .build());
      return presigned.url().toString();
    } catch (Exception e) {
      log.error("Error generating presigned download URL for Minio: {}/{}", bucketName, normalizedKey, e);
      throw new RuntimeException("Could not generate download URL", e);
    }
  }

  private S3Presigner buildPresigner() {
    String signingEndpoint = minioProperties.getPublicEndpoint();
    if (signingEndpoint == null || signingEndpoint.isBlank()) {
      signingEndpoint = minioProperties.getEndpoint();
    }

    URI uri = URI.create(signingEndpoint);
    if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
      signingEndpoint = uri.getScheme() + "://" + uri.getAuthority();
      log.warn("Stripped path from MinIO public endpoint for presign: {}", signingEndpoint);
    }

    return S3Presigner.builder()
        .endpointOverride(URI.create(signingEndpoint))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    minioProperties.getAccessKey(), minioProperties.getSecretKey())))
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  @Override
  public byte[] downloadFile(String key, String bucketName) {
    String normalizedKey = normalizeObjectKey(key, bucketName);
    try (InputStream inputStream =
        minioClient.getObject(
            GetObjectArgs.builder().bucket(bucketName).object(normalizedKey).build())) {
      return inputStream.readAllBytes();
    } catch (Exception e) {
      log.error("Error downloading file from Minio: {}/{}", bucketName, normalizedKey, e);
      throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
    }
  }

  private String normalizeObjectKey(String key, String bucketName) {
    if (key == null) {
      return null;
    }

    String value = key.trim();

    if (value.startsWith("http://") || value.startsWith("https://")) {
      try {
        String path = URI.create(value).getPath();
        value = path == null ? value : path;
      } catch (Exception ignored) {
        // Keep original value if URI parsing fails.
      }
    }

    while (value.startsWith("/")) {
      value = value.substring(1);
    }

    String bucketPrefix = bucketName + "/";
    if (value.startsWith(bucketPrefix)) {
      value = value.substring(bucketPrefix.length());
    }

    return value;
  }

  private void ensureBucketExists(String bucketName) throws Exception {
    boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    if (!found) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }
  }

  @Override
  public void deleteFile(String filePath) {
    // Current logic assumes template bucket, but we should handle both or specify bucket
    // For now, let's try template bucket as fallback
    String bucketName = minioProperties.getTemplateBucket();
    deleteFile(filePath, bucketName);
  }

  @Override
  public void deleteFile(String filePath, String bucketName) {
    String normalizedKey = normalizeObjectKey(filePath, bucketName);
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(normalizedKey).build());
      log.info("File deleted from Minio: {}/{}", bucketName, normalizedKey);
    } catch (Exception e) {
      log.error("Error deleting file from Minio: {}/{}", bucketName, normalizedKey, e);
    }
  }
}
