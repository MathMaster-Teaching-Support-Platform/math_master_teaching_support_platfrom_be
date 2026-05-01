package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.enums.HlsStatus;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.service.HlsTranscodingService;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Transcodes uploaded .mp4 videos into HLS format using FFmpeg (via Jaffree).
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Mark lesson as PROCESSING</li>
 *   <li>Download original .mp4 from MinIO to a temp file</li>
 *   <li>Run FFmpeg: mp4 → playlist.m3u8 + segment_XXX.ts (6-second chunks)</li>
 *   <li>Upload all generated files to MinIO under {courseId}/{lessonId}/hls/</li>
 *   <li>Update lesson: hlsStatus=READY, hlsPlaylistKey=...hls/playlist.m3u8</li>
 *   <li>Delete original .mp4 from MinIO (storage optimisation)</li>
 *   <li>Clean up temp dir</li>
 * </ol>
 *
 * <p>On any failure the lesson is marked FAILED and the original .mp4 is preserved
 * so the 2-hour presigned fallback in {@code getVideoPresignedUrl()} still works.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HlsTranscodingServiceImpl implements HlsTranscodingService {

  private final CourseLessonRepository courseLessonRepository;
  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  /** Segment duration in seconds (6 s ≈ Udemy standard). */
  private static final int SEGMENT_DURATION_SECONDS = 6;

  // ─── Public async entry point ─────────────────────────────────────────────

  @Override
  @Async("hlsTaskExecutor")
  public void transcodeAsync(UUID courseLessonId) {
    log.info("[HLS] Starting async transcode for lesson {}", courseLessonId);
    try {
      doTranscode(courseLessonId);
    } catch (Exception e) {
      log.error("[HLS] Transcode failed for lesson {}", courseLessonId, e);
      markFailed(courseLessonId);
    }
  }

  // ─── Core transcoding logic ───────────────────────────────────────────────

  private void doTranscode(UUID courseLessonId) throws Exception {
    // 1. Load lesson record
    CourseLesson cl = courseLessonRepository
        .findByIdAndDeletedAtIsNull(courseLessonId)
        .orElseThrow(() -> new IllegalArgumentException(
            "CourseLesson not found: " + courseLessonId));

    String bucket = minioProperties.getCourseVideosBucket();
    String sourceKey = cl.getVideoUrl(); // e.g. "019d.../36b1.mp4"

    if (sourceKey == null || sourceKey.isBlank()) {
      log.warn("[HLS] Lesson {} has no videoUrl – skipping transcode", courseLessonId);
      return;
    }

    // 2. Mark as PROCESSING
    markProcessing(courseLessonId);

    // 3. Create temp working directory
    Path tempDir = Files.createTempDirectory("hls_" + courseLessonId + "_");
    log.info("[HLS] Temp dir: {}", tempDir);

    try {
      // 4. Download source .mp4 from MinIO → temp file
      Path inputFile = tempDir.resolve("source.mp4");
      downloadFromMinio(bucket, sourceKey, inputFile);
      log.info("[HLS] Downloaded source: {} bytes", Files.size(inputFile));

      // 5. Run FFmpeg transcoding
      Path playlistFile = tempDir.resolve("playlist.m3u8");
      Path segmentPattern = tempDir.resolve("segment_%03d.ts");
      runFfmpeg(inputFile, playlistFile, segmentPattern, sourceKey);
      log.info("[HLS] FFmpeg completed. Files in {}: {}", tempDir,
          Files.list(tempDir).map(Path::getFileName).toList());

      // 6. Determine MinIO HLS prefix: same folder as source + /hls/
      // sourceKey = "019d.../36b1.mp4" → prefix = "019d.../hls/"
      String hlsPrefix = deriveHlsPrefix(sourceKey);

      // 7. Upload playlist.m3u8
      uploadToMinio(bucket, hlsPrefix + "playlist.m3u8", playlistFile, "application/vnd.apple.mpegurl");

      // 8. Upload all .ts segments
      try (Stream<Path> files = Files.list(tempDir)) {
        files.filter(p -> p.getFileName().toString().endsWith(".ts"))
            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
            .forEach(seg -> {
              String segKey = hlsPrefix + seg.getFileName().toString();
              uploadToMinio(bucket, segKey, seg, "video/MP2T");
            });
      }

      // 9. Update lesson record: READY
      String playlistKey = hlsPrefix + "playlist.m3u8";
      markReady(courseLessonId, playlistKey);
      log.info("[HLS] ✅ Lesson {} transcoded → {}", courseLessonId, playlistKey);

      // 10. Delete original .mp4 to save storage (best-effort)
      try {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(bucket)
            .object(sourceKey)
            .build());
        log.info("[HLS] Deleted original .mp4: {}", sourceKey);
      } catch (Exception e) {
        log.warn("[HLS] Could not delete original .mp4 {}: {}", sourceKey, e.getMessage());
      }

    } finally {
      // 11. Clean up temp directory
      deleteTempDir(tempDir);
    }
  }

  // ─── FFmpeg invocation ────────────────────────────────────────────────────

  private void runFfmpeg(Path inputFile, Path playlistFile, Path segmentPattern, String sourceKey) {
    // We MUST re-encode to guarantee HLS segment keyframe alignment. 
    // Using -codec copy on random MP4s causes broken HLS chunks because we can't control the keyframes.
    FFmpeg.atPath()
        .addInput(UrlInput.fromPath(inputFile))
        // 1. Video Codec & Profile (Maximum compatibility)
        .addArguments("-codec:v", "libx264")
        .addArguments("-profile:v", "main")
        .addArguments("-level", "4.0")
        .addArguments("-pix_fmt", "yuv420p")
        // 2. Quality & Bitrate (Constrained VBR)
        .addArguments("-preset", "fast")
        .addArguments("-crf", "22")
        .addArguments("-maxrate", "4000k")
        .addArguments("-bufsize", "8000k")
        // 3. Strict Framerate & GOP (Group of Pictures)
        // Normalize to 30fps. A GOP of 180 at 30fps guarantees a keyframe EXACTLY every 6 seconds.
        .addArguments("-r", "30")
        .addArguments("-g", "180")
        .addArguments("-keyint_min", "180")
        .addArguments("-sc_threshold", "0") // Disable scene cut detection
        .addArguments("-flags", "+cgop") // Closed GOP for independent segments
        // 4. Audio Codec & Sync
        .addArguments("-codec:a", "aac")
        .addArguments("-b:a", "128k")
        .addArguments("-ar", "44100")
        .addArguments("-af", "aresample=async=1") // Keep audio strictly synced with video
        // 5. HLS Packaging Flags
        .addArguments("-hls_time", String.valueOf(SEGMENT_DURATION_SECONDS))
        .addArguments("-hls_playlist_type", "vod")
        .addArguments("-hls_flags", "independent_segments") // CRITICAL for smooth decoding
        .addArguments("-hls_segment_filename", segmentPattern.toString())
        .addOutput(UrlOutput.toPath(playlistFile))
        .execute();
  }

  // ─── MinIO helpers ────────────────────────────────────────────────────────

  private void downloadFromMinio(String bucket, String key, Path dest) throws Exception {
    try (var stream = minioClient.getObject(
        GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      Files.copy(stream, dest);
    }
  }

  private void uploadToMinio(String bucket, String key, Path file, String contentType) {
    try {
      byte[] bytes = Files.readAllBytes(file);
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(bucket)
          .object(key)
          .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
          .contentType(contentType)
          .build());
      log.debug("[HLS] Uploaded {} ({} bytes)", key, bytes.length);
    } catch (Exception e) {
      throw new RuntimeException("Failed to upload HLS file to MinIO: " + key, e);
    }
  }

  // ─── DB state helpers ─────────────────────────────────────────────────────

  @Transactional
  protected void markProcessing(UUID id) {
    courseLessonRepository.findByIdAndDeletedAtIsNull(id).ifPresent(cl -> {
      cl.setHlsStatus(HlsStatus.PROCESSING);
      courseLessonRepository.save(cl);
    });
  }

  @Transactional
  protected void markReady(UUID id, String playlistKey) {
    courseLessonRepository.findByIdAndDeletedAtIsNull(id).ifPresent(cl -> {
      cl.setHlsStatus(HlsStatus.READY);
      cl.setHlsPlaylistKey(playlistKey);
      courseLessonRepository.save(cl);
    });
  }

  @Transactional
  protected void markFailed(UUID id) {
    courseLessonRepository.findByIdAndDeletedAtIsNull(id).ifPresent(cl -> {
      cl.setHlsStatus(HlsStatus.FAILED);
      courseLessonRepository.save(cl);
    });
  }

  // ─── Utility ─────────────────────────────────────────────────────────────

  /**
   * Derives the MinIO HLS prefix from the original .mp4 object key.
   * <p>
   * Example: {@code "019d.../36b1.mp4"} → {@code "019d.../hls/"}
   */
  private String deriveHlsPrefix(String sourceKey) {
    // Strip the filename from the key to get the directory, then append hls/
    int lastSlash = sourceKey.lastIndexOf('/');
    String dir = lastSlash >= 0 ? sourceKey.substring(0, lastSlash + 1) : "";
    return dir + "hls/";
  }

  private void deleteTempDir(Path dir) {
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
      });
    } catch (Exception e) {
      log.warn("[HLS] Failed to clean up temp dir {}: {}", dir, e.getMessage());
    }
  }
}
