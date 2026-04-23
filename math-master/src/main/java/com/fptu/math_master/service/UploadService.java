package com.fptu.math_master.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
  /**
   * Upload a file to Minio
   * @param file The file to upload
   * @param directory The subdirectory/prefix within the bucket
   * @return The URL or path to the stored file
   */
  String uploadFile(MultipartFile file, String directory);

  /**
   * Upload a file to Minio specifying a bucket
   * @param file The file to upload
   * @param directory The subdirectory/prefix
   * @param bucketName The bucket to upload to
   * @return The URL or path to the stored file
   */
  String uploadFile(MultipartFile file, String directory, String bucketName);

  /**
   * Upload multiple files to Minio as a single Zip file
   * @param files The list of files to upload
   * @param directory The subdirectory/prefix
   * @param zipName The base name for the zip file
   * @return The URL or path to the stored zip file
   */
  String uploadFilesAsZip(java.util.List<MultipartFile> files, String directory, String zipName);

  /**
   * Get a pre-signed URL for downloading a file
   * @param key Path/Key to the file
   * @param bucketName The bucket containing the file
   * @return A pre-signed URL valid for a short period
   */
  String getPresignedUrl(String key, String bucketName);

  /**
   * Get a pre-signed URL that forces the browser to download the file (Content-Disposition: attachment).
   * @param key        Object key in the bucket
   * @param bucketName The bucket containing the file
   * @param fileName   The suggested file name shown in the browser Save-As dialog
   * @return A pre-signed URL valid for a short period
   */
  String getPresignedDownloadUrl(String key, String bucketName, String fileName);

  /**
   * Download a file from Minio
   * @param key Path/Key to the file
   * @param bucketName The bucket containing the file
   * @return The byte array of the file content
   */
  byte[] downloadFile(String key, String bucketName);

  /**
   * Delete a file from Minio
   * @param filePath Path/Key to the file
   */
  void deleteFile(String filePath);

  /**
   * Delete a file from Minio in a specific bucket
   * @param filePath Path/Key to the file
   * @param bucketName The bucket containing the file
   */
  void deleteFile(String filePath, String bucketName);
}
