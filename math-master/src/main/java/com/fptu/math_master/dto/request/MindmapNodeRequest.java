package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MindmapNodeRequest {

  @NotNull(message = "Mindmap ID is required")
  private UUID mindmapId;

  private UUID parentId;

  @NotBlank(message = "Content is required")
  private String content;

  @Size(max = 50, message = "Color must not exceed 50 characters")
  private String color;

  @Size(max = 100, message = "Icon must not exceed 100 characters")
  private String icon;

  private Integer displayOrder;
}
