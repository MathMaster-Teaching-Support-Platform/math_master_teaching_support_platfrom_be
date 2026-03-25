package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryInfoResponse {
  private Integer wordLimit;
  private Integer currentWords;
  private Integer messageCount;
  private Boolean trimmed;
}
