package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.UploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    try {
      return minioClient.getPresignedObjectUrl(
          io.minio.GetPresignedObjectUrlArgs.builder()
              .method(io.minio.http.Method.GET)
              .bucket(bucketName)
              .object(key)
              .expiry(60 * 60) // 1 hour
              .build());
    } catch (Exception e) {
      log.error("Error generating pre-signed URL for Minio: {}/{}", bucketName, key, e);
      throw new RuntimeException("Could not generate download URL", e);
    }
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
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(filePath).build());
      log.info("File deleted from Minio: {}/{}", bucketName, filePath);
    } catch (Exception e) {
      log.error("Error deleting file from Minio: {}", filePath, e);
    }
  }
}
