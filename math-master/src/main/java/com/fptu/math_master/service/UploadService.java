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
   * Upload multiple files to Minio as a single Zip file
   * @param files The list of files to upload
   * @param directory The subdirectory/prefix
   * @param zipName The base name for the zip file
   * @return The URL or path to the stored zip file
   */
  String uploadFilesAsZip(java.util.List<MultipartFile> files, String directory, String zipName);

  /**
   * Delete a file from Minio
   * @param filePath Path/Key to the file
   */
  void deleteFile(String filePath);
}
