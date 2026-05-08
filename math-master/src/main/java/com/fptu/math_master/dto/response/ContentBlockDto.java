package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One OCR-extracted block within a page (text / heading / formula / image / definition / example /
 * exercise / note). Variable-shape: only the relevant fields per type are populated.
 *
 * <p>This DTO mirrors the structure stored by the Python crawler in MongoDB; the BE only relays it
 * — the canonical schema lives in the Python service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlockDto {
  private Integer order;
  private String type;
  private String content;
  private String latex;
  private String label;
  private String imageUrl;
  private String imagePath;
  private String thumbnailUrl;
  private String caption;
  private String exerciseType;
  private String exerciseNum;
  private Double confidence;
  private String source;
}
