package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
  private UUID id;
  private UUID subjectId;
  private String title;
  private String description;
  private Integer orderIndex;
  /** true nếu chapter đã bị soft-delete (FE hiển thị màu khác). */
  @Builder.Default
  private boolean deleted = false;
  private Instant createdAt;
  private Instant updatedAt;
}
