package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.MinioProperties;
import io.minio.MinioClient;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

  private final MinioProperties minioProperties;

  /**
   * Internal MinIO client for backend operations (upload, delete, etc.)
   * Uses internal Docker network endpoint
   */
  @Bean
  @Primary
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(minioProperties.getEndpoint())
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }

  /**
   * Public MinIO client for generating presigned URLs
   * Uses public endpoint accessible from browser
   * 
   * Note: This client is ONLY used for generating presigned URLs.
   * It doesn't need to actually connect to MinIO - just needs correct endpoint for signature.
   */
  @Bean(name = "publicMinioClient")
  public MinioClient publicMinioClient() {
    String publicEndpoint = minioProperties.getPublicEndpoint();
    String internalEndpoint = minioProperties.getEndpoint();
    
    System.out.println("=== MinIO Config Debug ===");
    System.out.println("Internal Endpoint: " + internalEndpoint);
    System.out.println("Public Endpoint: " + publicEndpoint);
    
    // MUST use public endpoint for presigned URLs
    if (publicEndpoint == null || publicEndpoint.isBlank()) {
      System.out.println("ERROR: Public endpoint is null/blank! Using internal as fallback.");
      publicEndpoint = internalEndpoint;
    }

    publicEndpoint = normalizeEndpoint(publicEndpoint);
    
    System.out.println("Using endpoint for publicMinioClient: " + publicEndpoint);
    System.out.println("========================");
    
    // Create client with public endpoint
    // This client may not be able to connect (if public endpoint is external)
    // but it will generate correct presigned URLs with public hostname
    return MinioClient.builder()
        .endpoint(publicEndpoint)
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }

  private String normalizeEndpoint(String endpoint) {
    URI uri = URI.create(endpoint);
    if (uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath())) {
      return endpoint;
    }

    String normalized = uri.getScheme() + "://" + uri.getAuthority();
    System.out.println("WARN: Stripping path from MinIO public endpoint: " + endpoint + " -> " + normalized);
    return normalized;
  }
}
