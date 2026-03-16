package com.fptu.math_master.service;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  /**
   * Store a file in the local storage
   * @param file The file to store
   * @param directory The subdirectory within the base storage path
   * @return The path/URL to the stored file
   */
  String storeFile(MultipartFile file, String directory);

  /**
   * Delete a file from storage
   * @param filePath Path to the file
   */
  void deleteFile(String filePath);

  /**
   * Get the absolute path for a stored file
   * @param filePath Relative path
   * @return Absolute path
   */
  Path load(String filePath);
}
