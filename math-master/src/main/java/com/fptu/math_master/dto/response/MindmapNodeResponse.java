package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapNodeResponse {

  private UUID id;
  private UUID mindmapId;
  private UUID parentId;
  private String content;
  private String color;
  private String icon;
  private Integer displayOrder;
  private List<MindmapNodeResponse> children;
  private Instant createdAt;
  private Instant updatedAt;
}
