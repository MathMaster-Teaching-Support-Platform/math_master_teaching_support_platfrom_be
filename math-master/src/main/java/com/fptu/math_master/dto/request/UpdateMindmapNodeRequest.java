package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMindmapNodeRequest {

  // Kept for backward compatibility with clients still sending old payload.
  private UUID mindmapId;

  private UUID parentId;

  @Size(max = 2000, message = "Content must not exceed 2000 characters")
  private String content;

  @Size(max = 50, message = "Color must not exceed 50 characters")
  private String color;

  @Size(max = 100, message = "Icon must not exceed 100 characters")
  private String icon;

  private Integer displayOrder;
}