package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the page-mapping bulk request — ties a Lesson (Postgres) to a
 * page range inside the book's PDF.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMappingItem {

  @NotNull(message = "lessonId is required")
  private UUID lessonId;

  @NotNull(message = "pageStart is required")
  @Min(value = 1, message = "pageStart must be >= 1")
  private Integer pageStart;

  @NotNull(message = "pageEnd is required")
  @Min(value = 1, message = "pageEnd must be >= 1")
  private Integer pageEnd;
}
