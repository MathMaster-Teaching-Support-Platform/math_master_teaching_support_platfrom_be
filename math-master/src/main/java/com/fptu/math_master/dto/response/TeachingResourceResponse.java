package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.TeachingResourceType;
import java.time.Instant;
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
public class TeachingResourceResponse {
  private UUID id;
  private String name;
  private TeachingResourceType type;
  private String fileUrl;
  private UUID createdBy;
  private Instant createdAt;
}
