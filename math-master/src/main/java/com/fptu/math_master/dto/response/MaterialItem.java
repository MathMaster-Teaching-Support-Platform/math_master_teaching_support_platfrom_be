package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a single supplementary material item.
 * Stored as part of a JSON array in course_lessons.materials column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialItem {
    private String id; // UUID
    private String name;
    private String key; // MinIO object key
    private String contentType;
    private Long size;
    private Instant uploadedAt;
    private String url; // Presigned URL for download
}
