package com.fptu.math_master.service;

import java.util.UUID;

/**
 * Asynchronously transcodes uploaded course videos into HLS format.
 * After a multipart upload is finalized, call {@link #transcodeAsync(UUID)}
 * to kick off a non-blocking FFmpeg pipeline that produces a {@code playlist.m3u8}
 * and a set of {@code .ts} segment files stored in MinIO.
 */
public interface HlsTranscodingService {

  /**
   * Enqueue an asynchronous HLS transcode job for the given lesson.
   * Returns immediately; the actual work runs on the configured async executor.
   *
   * @param courseLessonId the ID of the {@code CourseLesson} whose video should be transcoded
   */
  void transcodeAsync(UUID courseLessonId);
}
