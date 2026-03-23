package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachResourcesToRoadmapTopicRequest {

  @NotNull(message = "INVALID_KEY")
  @NotEmpty(message = "INVALID_KEY")
  private List<UUID> resourceIds;
}
