package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Replace the entire page-mapping for a book in one transaction. The service
 * validates: every page range fits the book's OCR window, and pages do not
 * "go backwards" once items are sorted by their corresponding Lesson.orderIndex
 * (overlap on a single page is allowed: lessons[i].pageEnd may equal
 * lessons[i+1].pageStart).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPageMappingRequest {

  @NotEmpty(message = "mappings must not be empty")
  @Valid
  private List<PageMappingItem> mappings;
}
