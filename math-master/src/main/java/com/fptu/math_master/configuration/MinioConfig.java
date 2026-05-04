package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.MinioProperties;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Wires the two MinIO clients used by the app and verifies the public endpoint
 * is reachable from a browser.
 *
 * <p>The split is deliberate: the {@linkplain #minioClient() internal client}
 * runs uploads/deletes/streams from inside the docker network and uses the
 * internal hostname; the {@linkplain #publicMinioClient() public client}
 * exists solely to produce presigned URLs whose authority matches what a
 * browser will resolve. If both pointed at the internal hostname (as they did
 * before — silently, when {@code MINIO_PUBLIC_ENDPOINT} was unset) we'd hand
 * the browser URLs like {@code http://nginx-minio:9000/...} that no client can
 * resolve, and downloads would fail without an obvious cause.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

  private final MinioProperties minioProperties;
  private final Environment environment;

  @Bean
  @Primary
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(minioProperties.getEndpoint())
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }

  @Bean(name = "publicMinioClient")
  public MinioClient publicMinioClient() {
    String publicEndpoint = minioProperties.getPublicEndpoint();
    if (publicEndpoint == null || publicEndpoint.isBlank()) {
      publicEndpoint = minioProperties.getEndpoint();
    }
    publicEndpoint = normalizeEndpoint(publicEndpoint);
    log.info(
        "MinIO endpoints: internal={} public={}", minioProperties.getEndpoint(), publicEndpoint);
    return MinioClient.builder()
        .endpoint(publicEndpoint)
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }

  /**
   * Logs an error at startup if the public endpoint is missing or still pointing
   * at the docker-internal host while a non-dev profile is active. We don't fail
   * the boot — that would prevent recovery from a misconfigured environment —
   * but the operator gets a single, prominent line in the boot log instead of
   * silent download failures discovered hours later by users.
   */
  @PostConstruct
  void verifyPublicEndpoint() {
    String publicEndpoint = minioProperties.getPublicEndpoint();
    String internalEndpoint = minioProperties.getEndpoint();
    boolean isProd = environment.matchesProfiles("prod");

    if (publicEndpoint == null || publicEndpoint.isBlank()) {
      String msg =
          "MINIO_PUBLIC_ENDPOINT is not set; presigned URLs will use the docker-internal host '"
              + internalEndpoint
              + "' and browsers will fail to resolve them.";
      if (isProd) log.error(msg);
      else log.warn(msg);
      return;
    }

    String authority = URI.create(publicEndpoint).getAuthority();
    if (authority != null
        && (authority.startsWith("minio") || authority.startsWith("nginx-minio"))
        && isProd) {
      log.error(
          "MINIO_PUBLIC_ENDPOINT='{}' looks like a docker-internal host. External browsers will not"
              + " be able to download materials. Set it to the public domain (e.g. https://...).",
          publicEndpoint);
    }
  }

  private String normalizeEndpoint(String endpoint) {
    URI uri = URI.create(endpoint);
    if (uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath())) {
      return endpoint;
    }
    String normalized = uri.getScheme() + "://" + uri.getAuthority();
    log.warn("Stripping path from MinIO public endpoint: {} -> {}", endpoint, normalized);
    return normalized;
  }
}
