package com.fptu.math_master.enums;

/**
 * Tracks the HLS transcoding lifecycle for a CourseLesson video.
 *
 * <pre>
 *   PENDING    – upload complete, transcode job not yet started
 *   PROCESSING – FFmpeg is currently running
 *   READY      – HLS segments uploaded to MinIO; playlist.m3u8 is available
 *   FAILED     – transcoding failed; original .mp4 is used as fallback
 * </pre>
 */
public enum HlsStatus {
  PENDING,
  PROCESSING,
  READY,
  FAILED
}
