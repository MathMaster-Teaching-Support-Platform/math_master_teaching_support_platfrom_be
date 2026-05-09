package com.fptu.math_master.dto.response;

/**
 * Short-lived MinIO GET URL for an OCR block image stored under {@code books/{bookId}/page-images/}.
 * Same pattern as course thumbnails — browser loads via {@code <img src>} without Authorization.
 */
public record BookPageImagePresignedUrlResponse(String url) {}
