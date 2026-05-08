package com.fptu.math_master.dto.response;

/**
 * Result of uploading an image asset for an OCR content block.
 *
 * <p>{@code imageUrl} is a stable, admin-protected URL the FE can save into the block. {@code
 * imagePath} is the underlying MinIO object key — kept for parity with OCR-generated blocks where
 * {@code imagePath} is sometimes the only stored reference.
 */
public record BookPageImageResponse(String imageUrl, String imagePath) {}
